import { useEffect, useState, useCallback, useRef } from 'react'
import { RefreshCw, AlertTriangle, CheckCircle, Clock } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { PanelCard } from '../common/PanelCard'
import { gatewayClient } from '../../services/gatewayClient'

const POLL_MS = 10_000

const STATUS_STEPS = [
  { key: 'IDLE',                label: 'Idle' },
  { key: 'BUILDING_GRAPH',      label: 'Building graph' },
  { key: 'STARTING_CONTAINER',  label: 'Starting OTP container' },
  { key: 'HEALTH_CHECK',        label: 'Waiting for OTP to load' },
  { key: 'SWITCHING_TRAFFIC',   label: 'Switching traffic' },
  { key: 'DRAINING',            label: 'Draining old instance' },
  { key: 'COMPLETED',           label: 'Completed' },
  { key: 'FAILED',              label: 'Failed' },
]

function timeAgo(isoString) {
  if (!isoString) return 'never'
  const diff = Date.now() - new Date(isoString).getTime()
  const mins = Math.floor(diff / 60000)
  if (mins < 1) return 'just now'
  if (mins < 60) return `${mins}m ago`
  const hrs = Math.floor(mins / 60)
  if (hrs < 24) return `${hrs}h ago`
  const days = Math.floor(hrs / 24)
  return `${days}d ago`
}

function statusIndex(status) {
  const idx = STATUS_STEPS.findIndex((s) => s.key === status)
  return idx >= 0 ? idx : 0
}

export function GraphRebuildPanel() {
  const { t } = useTranslation('admin-dashboard')
  const [state, setState] = useState(null)
  const [loading, setLoading] = useState(true)
  const [triggering, setTriggering] = useState(false)
  const pollRef = useRef(null)

  const fetchState = useCallback(async () => {
    try {
      const data = await gatewayClient.getOtpRebuildState()
      setState(data)
    } catch {
      // silently ignore
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    fetchState()
  }, [fetchState])

  useEffect(() => {
    pollRef.current = setInterval(fetchState, POLL_MS)
    return () => clearInterval(pollRef.current)
  }, [fetchState])

  const handleTrigger = async () => {
    setTriggering(true)
    try {
      await gatewayClient.triggerOtpRebuild()
      await fetchState()
    } catch {
      // error handled by polling
    } finally {
      setTriggering(false)
    }
  }

  if (loading) {
    return (
      <PanelCard>
        <div className="animate-pulse rounded bg-border h-16" />
      </PanelCard>
    )
  }

  if (!state) return null

  const { needsRebuild, rebuildInProgress, status, lastDataChangeAt, lastRebuildAt, errorMessage } = state
  const currentStep = statusIndex(status)
  const totalSteps = STATUS_STEPS.length - 2
  const progress = rebuildInProgress ? Math.round((currentStep / totalSteps) * 100) : (status === 'COMPLETED' ? 100 : 0)

  return (
    <PanelCard>
      <div className="flex items-start justify-between gap-4">
        <div className="min-w-0 flex-1">
          <div className="flex items-center gap-2 mb-1">
            {needsRebuild || rebuildInProgress ? (
              <AlertTriangle size={16} className="text-amber-500 shrink-0" />
            ) : (
              <CheckCircle size={16} className="text-green-500 shrink-0" />
            )}
            <h3 className="text-sm font-semibold text-ink">OTP Graph Status</h3>
          </div>

          {rebuildInProgress ? (
            <div className="mt-2">
              <div className="flex items-center justify-between text-xs text-muted mb-1">
                <span>{STATUS_STEPS[currentStep]?.label ?? status}</span>
                <span>{progress}%</span>
              </div>
              <div className="w-full bg-border rounded-full h-1.5">
                <div
                  className="bg-accent h-1.5 rounded-full transition-all duration-500"
                  style={{ width: `${progress}%` }}
                />
              </div>
            </div>
          ) : needsRebuild ? (
            <p className="text-xs text-muted mt-0.5">
              Data changed {timeAgo(lastDataChangeAt)} &mdash; rebuild needed
            </p>
          ) : (
            <p className="text-xs text-muted mt-0.5">
              Graph is up to date &mdash; last rebuilt {timeAgo(lastRebuildAt)}
            </p>
          )}

          {status === 'FAILED' && errorMessage && (
            <p className="text-xs text-red-500 mt-1 truncate" title={errorMessage}>
              Error: {errorMessage}
            </p>
          )}
        </div>

        <div className="shrink-0">
          {rebuildInProgress ? (
            <div className="flex items-center gap-1.5 text-xs text-muted">
              <RefreshCw size={12} className="animate-spin" />
              <span>Rebuilding</span>
            </div>
          ) : (
            <button
              onClick={handleTrigger}
              disabled={!needsRebuild || triggering}
              className={`flex items-center gap-1.5 rounded-panel border px-3 py-1.5 text-xs font-medium transition ${
                needsRebuild && !triggering
                  ? 'border-accent bg-accent text-white hover:bg-accent/90'
                  : 'border-border text-muted cursor-not-allowed'
              }`}
            >
              {triggering ? (
                <RefreshCw size={12} className="animate-spin" />
              ) : (
                <Clock size={12} />
              )}
              Schedule Rebuild
            </button>
          )}
        </div>
      </div>
    </PanelCard>
  )
}

export default GraphRebuildPanel
