import { atom, read, update } from 'claude-code'
import type { EngineInterface, Register, SessionRateLimit } from 'claude-code'

import type { GuardStage, PendingPush } from '../types'

// 5시간 창 기준 임계값(%)
const WARN_AT = 85
const SAVE_AT = 95

// 자동 커밋을 하지 않는 공용 브랜치
const SHARED_BRANCHES = new Set(['main', 'master', 'develop'])

const PANE = 'limit-guard'

// 이번 창에서 이미 한 단계(none → warned → saved). 창이 리셋되면 none 으로 돌아간다
const stage = atom({ plugin: 'limit-guard', key: 'stage' } as const, 'none' as GuardStage)
// 커밋은 됐고 사람의 푸시 승인을 기다리는 상태
const pending = atom({ plugin: 'limit-guard', key: 'pending' } as const, null as PendingPush | null)

const fiveHour = (limits: readonly SessionRateLimit[]) =>
  limits.find(l => l.kind === 'five_hour')

/** 리셋 시각(ISO)으로 "약 1시간 20분 뒤 풀립니다" 문구를 만든다. 모르면 빈 문자열 */
const untilReset = (resetsAt?: string) => {
  if (!resetsAt) return ''
  const mins = Math.max(0, Math.round((Date.parse(resetsAt) - Date.now()) / 60_000))
  const h = Math.floor(mins / 60), mm = mins % 60
  return ` 한도는 약 ${h ? `${h}시간 ` : ''}${mm}분 뒤 풀립니다.`
}

const git = async ($: EngineInterface, root: string, ...args: string[]) =>
  $.process.run(['git', ...args], { cwd: root })

/** 핸드오프 파일에 중단 마커를 덧붙인다. 파일이 없으면 섹션 2개로 새로 만든다 */
const markHandoff = async ($: EngineInterface, root: string, name: string, pct: number, tracked: string[], untracked: string[]) => {
  const path = `${root}/handoff/${name}.md`
  // 사람이 겪은 시각 그대로 남기기 위해 로컬 시각으로 찍는다
  const stamp = new Date(Date.now() - new Date().getTimezoneOffset() * 60_000).toISOString().slice(0, 16).replace('T', ' ')
  const list = (files: string[]) => (files.length ? files.join(', ') : '없음')
  const block =
    `\n## 중단 ${stamp} (5시간 한도 ${pct}%)\n` +
    `- 자동 커밋한 변경 ${tracked.length}개: ${list(tracked)}\n` +
    `- 커밋 안 한 새 파일 ${untracked.length}개 (사람이 판단): ${list(untracked)}\n` +
    `- 이어서: 이 파일의 "지금 하는 것" 과 \`git log -1\` 을 본다\n`
  const current = (await $.fs.exists(path)) ? await $.fs.read(path) : `# ${name} 핸드오프\n\n## 지금 하는 것\n\n## 다음에 할 것\n`
  await $.fs.write(path, current + block)
  return path
}

/** 작업 트리를 보존한다: 마커 → 추적 파일만 커밋. 푸시는 사람 승인 뒤(pane 버튼 또는 /limit-guard push) */
const save = async ($: EngineInterface, pct: number, reset: string, rootOverride?: string) => {
  const root = rootOverride ?? await $.session.root()
  const inRepo = await git($, root, 'rev-parse', '--is-inside-work-tree')
  if (inRepo.exitCode !== 0) return $.ui.toast(`5시간 한도 ${pct}% — 곧 막힙니다. git 저장소가 아니라 자동 저장은 못 했습니다.${reset}`)

  const branch = (await git($, root, 'branch', '--show-current')).stdout.trim()
  if (!branch || SHARED_BRANCHES.has(branch)) {
    return $.ui.toast(`5시간 한도 ${pct}% — 곧 막힙니다. '${branch || 'detached'}' 는 공용 브랜치라 자동 저장 안 함. 개인 브랜치로 옮겨 직접 커밋·푸시하세요.${reset}`)
  }

  // 팀 규칙: 브랜치 이름 = 이니셜. 핸드오프 파일명과 커밋 접두사 둘 다 여기서 나온다
  const initials = branch.split('/')[0] ?? branch
  const status = (await git($, root, 'status', '--porcelain')).stdout.split('\n').filter(Boolean)
  const untracked = status.filter(l => l.startsWith('??')).map(l => l.slice(3))
  const tracked = status.filter(l => !l.startsWith('??')).map(l => l.slice(3))
  // 추적 중인 변경이 없으면 커밋할 것이 없다 (재시작 뒤 같은 창에서 다시 95% 를 만나는 경우)
  if (tracked.length === 0) {
    return $.ui.toast(`5시간 한도 ${pct}% — 곧 막힙니다. 커밋할 변경이 없습니다${untracked.length ? ` (새 파일 ${untracked.length}개는 사람이 판단)` : ''}.${reset}`)
  }
  await markHandoff($, root, initials, pct, tracked, untracked)

  // 공개 저장소이므로 추적 중인 파일만 올린다. 새 파일은 마커에만 적고 사람이 결정한다
  await git($, root, 'add', '-u')
  await git($, root, 'add', `handoff/${initials}.md`)
  const committed = await git($, root, 'commit', '-m', `[${initials}] chore: 한도 ${pct}% 자동 저장 (${tracked.length}개 파일)`)
  if (committed.exitCode !== 0) return $.ui.toast(`5시간 한도 ${pct}% — 곧 막힙니다. 자동 커밋 실패: ${committed.stderr.trim().slice(0, 120)}`)

  await update($, pending, () => ({ root, branch, pct }))
  $.ui.toast(`5시간 한도 ${pct}% — 곧 막힙니다. '${branch}' 에 커밋했습니다. 푸시는 limit-guard 패널에서 승인하세요.${reset}`)
  await $.ui.open({ id: PANE, title: 'limit-guard' })
}

/** 사람이 승인한 푸시 */
const pushPending = async ($: EngineInterface) => {
  const p = await read($, pending)
  if (!p) return $.ui.toast('푸시 대기 중인 커밋이 없습니다')
  const pushed = await git($, p.root, 'push', '-u', 'origin', p.branch)
  if (pushed.exitCode === 0) {
    await update($, pending, () => null)
    await $.ui.close({ id: PANE })
    $.ui.toast(`'${p.branch}' 푸시 완료. 한도가 풀리면 어디서든 pull 해서 이어가세요.`)
  } else {
    $.ui.toast(`push 실패: ${pushed.stderr.trim().slice(0, 120)}`)
  }
}

/** 5시간 창 측정값 하나에 반응한다. 단계마다 창당 한 번 */
const onPercent = async ($: EngineInterface, pct: number, resetsAt?: string, rootOverride?: string) => {
  const reset = untilReset(resetsAt)
  const done = await read($, stage)
  if (pct >= SAVE_AT && done !== 'saved') {
    await update($, stage, () => 'saved')
    await save($, pct, reset, rootOverride)
  } else if (pct >= SAVE_AT && (await read($, pending))) {
    // 이미 커밋했고 푸시 승인만 남았으면 닫힌 패널을 다시 연다
    await $.ui.open({ id: PANE, title: 'limit-guard' })
  } else if (pct >= WARN_AT && done === 'none') {
    await update($, stage, () => 'warned')
    $.ui.toast(`5시간 한도 ${pct}% — 남은 양 ${100 - pct}%. ${SAVE_AT}% 가 되면 작업을 자동 커밋합니다. 지금 마무리 지점을 잡으세요.${reset}`)
  } else if (pct < WARN_AT && done !== 'none') {
    // 창이 리셋됨: 다음 창을 위해 처음 상태로
    await update($, stage, () => 'none')
  }
}

export const register: Register = on => {
  on('session.start', async ($, e, next) => {
    await $.command.register({
      name: 'limit-guard',
      description: '5시간 한도 측정값을 가짜로 흘려 동작을 본다 (85 → 경고, 95 → 커밋) / push → 대기 중인 커밋 푸시 / reset → 처음 상태로',
      argumentHint: '<percent> [repo-path] | push | reset',
    })
    return next(e)
  })

  on('command.run', { command: 'limit-guard' }, async ($, e) => {
    const arg = e.args.trim()
    if (arg === 'reset') {
      await update($, stage, () => 'none')
      $.ui.toast('5시간 한도 감시를 처음 상태로 되돌렸습니다. 85% 경고와 95% 자동 저장이 다시 한 번씩 동작합니다.')
      return { text: '감시 초기화 (stage=none)' }
    }
    if (arg === 'push') {
      await pushPending($)
      return { text: (await read($, pending)) ? '푸시 안 됨 (대기 유지)' : '푸시 완료 또는 대기 없음' }
    }
    const [first, repo] = arg.split(/\s+/)
    const pct = Number(first)
    if (!Number.isFinite(pct) || pct < 0 || pct > 100) return { text: '0~100 숫자 [레포 경로] | push | reset' }
    await onPercent($, pct, undefined, repo || undefined)
    return { text: `가짜 측정 ${pct}% 처리 (stage=${await read($, stage)})` }
  })

  on('session.measure', async ($, e, next) => {
    const window = fiveHour(e.rateLimits)
    if (window) await onPercent($, window.percentUsed, window.resetsAt)
    return next(e)
  }).catch((_$, e, next) => next(e))

  on('ui.render', { component: 'Pane', requestId: PANE }, async ($, e) => {
    const { Box, Text, Button } = $.ui.resolve(e)
    const p = await read($, pending)
    return (
      <Box flexDirection="column">
        {p === null && <Text dimColor>푸시 대기 중인 커밋이 없습니다.</Text>}
        {p !== null && (
          <>
            <Text>5시간 한도 {p.pct}% — '{p.branch}' 에 커밋해 두었습니다.</Text>
            <Text dimColor>origin 으로 푸시하면 다른 PC 에서 pull 해서 이어갈 수 있습니다.</Text>
            <Box gap={1}>
              <Button variant="primary" hotkey="y" onPress={() => void pushPending($)}>푸시</Button>
              <Button role="dismiss" hotkey="n" onPress={() => void $.ui.close({ id: PANE })}>나중에 (/limit-guard push)</Button>
            </Box>
          </>
        )}
      </Box>
    )
  })
}
