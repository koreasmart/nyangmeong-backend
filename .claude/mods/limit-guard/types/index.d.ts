/** 이번 5시간 창에서 limit-guard 가 이미 한 단계. 각 단계는 창당 한 번만 */
export type GuardStage = 'none' | 'warned' | 'saved'

/** 자동 커밋은 됐고 사람의 푸시 승인을 기다리는 상태 */
export type PendingPush = { root: string; branch: string; pct: number }

declare module 'claude-code' {
  interface PluginState {
    'limit-guard': {
      stage: GuardStage
      pending: PendingPush | null
    }
  }
}
