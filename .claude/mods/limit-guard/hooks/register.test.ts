import { expect, test } from 'claude-code/testing'
import type { Engine } from 'claude-code/testing'
import type { On } from 'claude-code'

type World = { branch: string; toasts: string[]; gitCalls: string[][]; written: Record<string, string>; opens: number }

/** Fakes git, the file system and toasts beneath the plugin; returns what it saw. */
const world = (on: On, branch: string, statusOut = ' M src/a.js\n?? src/b.js\n'): World => {
  const w: World = { branch, toasts: [], gitCalls: [], written: {}, opens: 0 }
  on('ui.toast', (_$, e) => { w.toasts.push(e.text); return { value: undefined } })
  on('session.root', () => ({ value: '/repo' }))
  on('session.measure', (_$, e) => ({ changed: e.changed }))
  on('fs.exists', () => ({ value: false }))
  on('ui.open', () => { w.opens += 1; return { value: { isPlaced: true as const } } })
  on('ui.close', () => ({ value: undefined }))
  on('fs.write', (_$, e) => { w.written[e.path] = e.text; return { value: undefined } })
  on('process.run', (_$, e) => {
    const [cmd, ...args] = e.argv
    if (cmd === 'git') w.gitCalls.push(args)
    const ok = (stdout = '') => ({ value: { exitCode: 0, stdout, stderr: '', isStdoutTruncated: false, isStderrTruncated: false } })
    switch (args[0]) {
      case 'rev-parse': return ok('true\n')
      case 'branch': return ok(`${w.branch}\n`)
      case 'config': return ok('jinseong\n')
      case 'status': return ok(statusOut)
      default: return ok()
    }
  })
  return w
}

const run = ($: Engine, args: string) =>
  $.command.run({ command: 'limit-guard', args, origin: { kind: 'composer' }, presentation: { isFullscreen: false, columns: 120 } })

const measure = ($: Engine, percentUsed: number) =>
  $.session.measure({
    context: { window: 200000 },
    rateLimits: [{ kind: 'five_hour', percentUsed }],
    changed: ['rateLimits'],
  })

test('85% 는 경고 토스트 한 번, 저장은 안 한다', async ($, on) => {
  const w = world(on, 'LJS/login')
  await measure($, 85)
  await measure($, 86)
  expect(w.toasts).toHaveLength(1)
  expect(w.toasts[0]).toContain('85%')
  expect(w.toasts[0]).toContain('남은 양 15%')
  expect(w.gitCalls).toHaveLength(0)
})

test('95% 개인 브랜치: 마커 → add -u → 커밋까지만, 푸시는 승인 뒤 (/limit-guard push)', async ($, on) => {
  const w = world(on, 'LJS/login')
  await measure($, 95)
  await measure($, 96)
  const sub = w.gitCalls.map(a => a[0])
  expect(sub).toEqual(['rev-parse', 'branch', 'status', 'add', 'add', 'commit'])
  expect(w.gitCalls.find(a => a[0] === 'add')).toEqual(['add', '-u'])
  expect(w.gitCalls.filter(a => a[0] === 'add')[1]).toEqual(['add', 'handoff/LJS.md'])
  expect(w.gitCalls.find(a => a[0] === 'commit')?.[2]).toMatch(/^\[LJS\] chore: 한도 95% 자동 저장 \(1개 파일\)$/)
  const marker = w.written['/repo/handoff/LJS.md']
  expect(marker).toContain('## 중단')
  expect(marker).toContain('자동 커밋한 변경 1개: src/a.js')
  expect(marker).toContain('커밋 안 한 새 파일 1개 (사람이 판단): src/b.js')
  expect(w.toasts[w.toasts.length - 1]).toContain('패널에서 승인')
  expect(w.gitCalls.find(a => a[0] === 'push')).toBeUndefined()
  // 두 번째 95 는 커밋을 또 하지 않고 패널만 다시 연다
  expect(w.opens).toBe(2)
  expect(w.gitCalls.filter(a => a[0] === 'commit')).toHaveLength(1)

  const pushed = await run($, 'push')
  expect(w.gitCalls.find(a => a[0] === 'push')).toEqual(['push', '-u', 'origin', 'LJS/login'])
  expect(pushed.text).toContain('푸시 완료')
  expect(w.toasts[w.toasts.length - 1]).toContain('푸시 완료')
})

test('추적 변경이 없으면 95% 에도 커밋하지 않는다', async ($, on) => {
  const w = world(on, 'LJS', '?? only-new.txt\n')
  await measure($, 95)
  expect(w.gitCalls.find(a => a[0] === 'commit')).toBeUndefined()
  expect(Object.keys(w.written)).toHaveLength(0)
  expect(w.toasts[w.toasts.length - 1]).toContain('커밋할 변경이 없습니다 (새 파일 1개는 사람이 판단)')
})

test('대기 중인 커밋이 없을 때 push 는 안내만', async ($, on) => {
  const w = world(on, 'LJS')
  await run($, 'push')
  expect(w.gitCalls).toHaveLength(0)
  expect(w.toasts[0]).toContain('없습니다')
})

test('95% 공용 브랜치(develop): 커밋·푸시 안 하고 안내만', async ($, on) => {
  const w = world(on, 'develop')
  await measure($, 95)
  const sub = w.gitCalls.map(a => a[0])
  expect(sub).toEqual(['rev-parse', 'branch'])
  expect(w.toasts[w.toasts.length - 1]).toContain('공용 브랜치')
  expect(Object.keys(w.written)).toHaveLength(0)
})

test('창이 리셋돼 85% 아래로 내려오면 경고가 다시 켜진다', async ($, on) => {
  const w = world(on, 'LJS/login')
  await measure($, 90)
  await measure($, 3)
  await measure($, 88)
  expect(w.toasts.filter(t => t.includes('자동 커밋합니다'))).toHaveLength(2)
})

test('/limit-guard 95 는 실제 측정과 같은 경로를 탄다, reset 은 경고·저장 다시 켜기', async ($, on) => {
  const w = world(on, 'LJS/login')
  const ran = await run($, '95 /other')
  expect(ran.text).toContain('stage=saved')
  expect(w.gitCalls.map(a => a[0])).toContain('commit')
  expect(w.written['/other/handoff/LJS.md']).toContain('## 중단')
  const reset = await run($, 'reset')
  expect(reset.text).toContain('stage=none')
  expect(w.toasts[w.toasts.length - 1]).toContain('처음 상태로')
  const bad = await run($, 'abc')
  expect(bad.text).toContain('0~100')
})

test('resetsAt 이 오면 풀리는 시각이 토스트에 붙는다', async ($, on) => {
  const w = world(on, 'LJS')
  const resetsAt = new Date(Date.now() + 80 * 60_000).toISOString()
  await $.session.measure({ context: { window: 200000 }, rateLimits: [{ kind: 'five_hour', percentUsed: 85, resetsAt }], changed: ['rateLimits'] })
  expect(w.toasts[0]).toMatch(/약 1시간 (19|20|21)분 뒤 풀립니다/)
})

test('five_hour 창이 없으면 아무것도 안 한다', async ($, on) => {
  const w = world(on, 'LJS/login')
  await $.session.measure({ context: { window: 200000 }, rateLimits: [{ kind: 'seven_day', percentUsed: 99 }], changed: ['rateLimits'] })
  expect(w.toasts).toHaveLength(0)
  expect(w.gitCalls).toHaveLength(0)
})
