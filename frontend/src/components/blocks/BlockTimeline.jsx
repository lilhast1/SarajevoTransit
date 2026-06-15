import { useMemo, useState } from 'react'
import { Clock, MapPin, X } from 'lucide-react'

const PX_PER_MINUTE = 2.6
const LANE_HEIGHT    = 54
const LABEL_WIDTH    = 136
const AXIS_HEIGHT    = 40
const BAR_INSET      = 8

const DIR_PALETTE = [
  { accent: '#60a5fa', bg: 'rgba(96,165,250,0.10)',  ring: 'rgba(96,165,250,0.38)',  text: '#93c5fd'  },
  { accent: '#fbbf24', bg: 'rgba(251,191,36,0.10)',  ring: 'rgba(251,191,36,0.38)',  text: '#fde68a'  },
  { accent: '#34d399', bg: 'rgba(52,211,153,0.10)',  ring: 'rgba(52,211,153,0.38)',  text: '#6ee7b7'  },
  { accent: '#f472b6', bg: 'rgba(244,114,182,0.10)', ring: 'rgba(244,114,182,0.38)', text: '#fbcfe8'  },
  { accent: '#a78bfa', bg: 'rgba(167,139,250,0.10)', ring: 'rgba(167,139,250,0.38)', text: '#ddd6fe'  },
  { accent: '#22d3ee', bg: 'rgba(34,211,238,0.10)',  ring: 'rgba(34,211,238,0.38)',  text: '#a5f3fc'  },
]

function toMin(t) {
  if (!t) return 0
  const [h, m] = String(t).split(':').map(Number)
  return h * 60 + (m || 0)
}

function fmt(t) {
  if (!t) return '\u2014'
  const s = String(t)
  return s.length >= 8 ? s.slice(0, 5) : s.length >= 5 ? s.slice(0, 5) : s
}

function layoverBetween(prevArrival, nextDeparture) {
  if (!prevArrival || !nextDeparture) return null
  const diff = toMin(nextDeparture) - toMin(prevArrival)
  return diff >= 0 ? diff : null
}

function hexToRgb(hex) {
  const r = parseInt(hex.slice(1, 3), 16)
  const g = parseInt(hex.slice(3, 5), 16)
  const b = parseInt(hex.slice(5, 7), 16)
  return `${r},${g},${b}`
}

export function BlockTimeline({ blocks, t }) {
  const [selectedTrip, setSelectedTrip] = useState(null)
  const tf = typeof t === 'function' ? t : (k => k)

  const { minMin, hourMarkers, totalWidth } = useMemo(() => {
    let lo = Infinity, hi = 0
    for (const b of blocks) {
      for (const trip of b.trips || []) {
        const d = toMin(trip.departureTime)
        const a = toMin(trip.arrivalTime)
        if (d < lo) lo = d
        if (a > hi) hi = a
      }
    }
    if (!isFinite(lo)) lo = 0
    if (hi === 0) hi = 720
    lo = Math.max(0, Math.floor(lo / 60) * 60 - 30)
    hi = Math.ceil(hi / 60) * 60 + 30

    const markers = []
    for (let m = lo; m <= hi; m += 60) {
      markers.push({
        label: `${String(Math.floor(m / 60)).padStart(2, '0')}:00`,
        pos: (m - lo) * PX_PER_MINUTE,
      })
    }
    return { minMin: lo, hourMarkers: markers, totalWidth: (hi - lo) * PX_PER_MINUTE }
  }, [blocks])

  const xOf = (time) => (toMin(time) - minMin) * PX_PER_MINUTE

  const selectedBlock = selectedTrip
    ? blocks.find(b => (b.trips || []).some(tr => tr.timetableId === selectedTrip.timetableId))
    : null
  const sp = selectedTrip
    ? DIR_PALETTE[(selectedTrip.directionId || 0) % DIR_PALETTE.length]
    : null

  return (
    <div style={{
      background: '#0d1117',
      borderRadius: 12,
      overflow: 'hidden',
      fontFamily: 'system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif',
    }}>
      <style>{`
        .bt-bar { transition: filter .12s, box-shadow .12s; }
        .bt-bar:hover { filter: brightness(1.2); }
        .bt-lane { transition: background .1s; }
        .bt-lane:hover { background: #161b22 !important; }
        .bt-close { border-radius: 5px; transition: background .1s, color .1s; }
        .bt-close:hover { background: rgba(255,255,255,0.08); color: #adbac7 !important; }
      `}</style>

      <div style={{ overflowX: 'auto' }}>
        <div style={{ minWidth: totalWidth + LABEL_WIDTH + 24, position: 'relative' }}>

          <div style={{
            display: 'flex',
            height: AXIS_HEIGHT,
            position: 'sticky',
            top: 0,
            zIndex: 20,
            background: '#0d1117',
            borderBottom: '1px solid #1c2128',
          }}>
            <div style={{
              width: LABEL_WIDTH,
              flexShrink: 0,
              display: 'flex',
              alignItems: 'flex-end',
              paddingLeft: 16,
              paddingBottom: 8,
            }}>
              <span style={{
                fontSize: 10,
                fontWeight: 700,
                letterSpacing: '0.10em',
                textTransform: 'uppercase',
                color: '#484f58',
                userSelect: 'none',
              }}>BLOCK</span>
            </div>

            <div style={{ flex: 1, position: 'relative' }}>
              {hourMarkers.map((m, i) => (
                <div key={i} style={{ position: 'absolute', left: m.pos, top: 0, bottom: 0 }}>
                  <div style={{
                    position: 'absolute',
                    bottom: 0,
                    left: 0,
                    width: 1,
                    height: 8,
                    background: '#21262d',
                  }} />
                  <span style={{
                    position: 'absolute',
                    bottom: 10,
                    left: 5,
                    fontSize: 10.5,
                    fontWeight: 500,
                    fontFamily: 'ui-monospace, SFMono-Regular, "SF Mono", Consolas, monospace',
                    color: '#484f58',
                    letterSpacing: '0.03em',
                    whiteSpace: 'nowrap',
                    userSelect: 'none',
                  }}>{m.label}</span>
                </div>
              ))}
            </div>
          </div>

          <div style={{
            position: 'absolute',
            left: LABEL_WIDTH,
            right: 0,
            top: AXIS_HEIGHT,
            bottom: 0,
            pointerEvents: 'none',
            zIndex: 0,
          }}>
            {hourMarkers.map((m, i) => (
              <div key={i} style={{
                position: 'absolute',
                left: m.pos,
                top: 0,
                bottom: 0,
                width: 1,
                background: '#161b22',
              }} />
            ))}
          </div>

          {blocks.map((block) => (
            <div
              key={block.blockId}
              className="bt-lane"
              style={{
                display: 'flex',
                height: LANE_HEIGHT,
                alignItems: 'center',
                borderBottom: '1px solid #1c2128',
                position: 'relative',
                zIndex: 1,
                background: '#0d1117',
              }}
            >
              <div style={{ width: LABEL_WIDTH, flexShrink: 0, paddingLeft: 16, paddingRight: 12 }}>
                <span style={{
                  display: 'inline-block',
                  background: '#161b22',
                  border: '1px solid #30363d',
                  borderRadius: 5,
                  padding: '3px 8px',
                  fontSize: 11,
                  fontWeight: 600,
                  fontFamily: 'ui-monospace, SFMono-Regular, "SF Mono", Consolas, monospace',
                  color: '#768390',
                  maxWidth: LABEL_WIDTH - 28,
                  overflow: 'hidden',
                  textOverflow: 'ellipsis',
                  whiteSpace: 'nowrap',
                }}>
                  {block.blockId}
                </span>
              </div>

              <div style={{ flex: 1, position: 'relative', height: '100%' }}>
                {(block.trips || []).map((trip, idx) => {
                  const c = DIR_PALETTE[(trip.directionId || 0) % DIR_PALETTE.length]
                  const depX = xOf(trip.departureTime)
                  const arrX = xOf(trip.arrivalTime)
                  const width = Math.max(arrX - depX, 26)
                  const prevTrip = idx > 0 ? block.trips[idx - 1] : null
                  const layover = prevTrip ? layoverBetween(prevTrip.arrivalTime, trip.departureTime) : null
                  const isSelected = selectedTrip?.timetableId === trip.timetableId
                  const showFull = width >= 96
                  const showCompact = width >= 40 && !showFull

                  return (
                    <div key={trip.timetableId} style={{ position: 'absolute', inset: 0, pointerEvents: 'none' }}>

                      {prevTrip && layover !== null && layover > 0 && (() => {
                        const prevEndX = xOf(prevTrip.arrivalTime)
                        const connW = depX - prevEndX
                        if (connW < 4) return null
                        return (
                          <div style={{
                            position: 'absolute',
                            left: prevEndX,
                            width: connW,
                            top: '50%',
                            transform: 'translateY(-50%)',
                            pointerEvents: 'none',
                          }}>
                            <div style={{
                              height: 1,
                              background: 'repeating-linear-gradient(90deg, #2d333b 0px, #2d333b 4px, transparent 4px, transparent 9px)',
                            }} />
                            {connW > 52 && (
                              <span style={{
                                position: 'absolute',
                                left: '50%',
                                top: -13,
                                transform: 'translateX(-50%)',
                                fontSize: 9,
                                fontFamily: 'ui-monospace, SFMono-Regular, "SF Mono", Consolas, monospace',
                                color: '#484f58',
                                background: '#0d1117',
                                padding: '1px 5px',
                                borderRadius: 3,
                                border: '1px solid #21262d',
                                whiteSpace: 'nowrap',
                                letterSpacing: '0.01em',
                                pointerEvents: 'none',
                              }}>{layover}m</span>
                            )}
                          </div>
                        )
                      })()}

                      <button
                        type="button"
                        className="bt-bar"
                        onClick={() => setSelectedTrip(isSelected ? null : trip)}
                        style={{
                          position: 'absolute',
                          left: depX,
                          width,
                          top: BAR_INSET,
                          height: LANE_HEIGHT - BAR_INSET * 2,
                          borderRadius: 5,
                          border: `1px solid ${c.accent}30`,
                          borderLeft: `3px solid ${c.accent}`,
                          background: isSelected
                            ? `linear-gradient(90deg, ${c.bg.replace('0.10', '0.22')} 0%, ${c.bg.replace('0.10', '0.06')} 100%)`
                            : `linear-gradient(90deg, ${c.bg} 0%, rgba(0,0,0,0) 100%)`,
                          display: 'flex',
                          alignItems: 'center',
                          padding: '0 8px 0 9px',
                          cursor: 'pointer',
                          pointerEvents: 'auto',
                          overflow: 'hidden',
                          boxShadow: isSelected
                            ? `0 0 0 1.5px ${c.accent}55, 0 0 18px ${c.ring}`
                            : '0 1px 2px rgba(0,0,0,0.5)',
                          outline: 'none',
                        }}
                      >
                        {showFull && (
                          <span style={{
                            fontSize: 10,
                            fontFamily: 'ui-monospace, SFMono-Regular, "SF Mono", Consolas, monospace',
                            color: c.text,
                            lineHeight: 1.35,
                            whiteSpace: 'nowrap',
                            overflow: 'hidden',
                            textOverflow: 'ellipsis',
                            letterSpacing: '0.01em',
                          }}>
                            <span style={{ fontWeight: 700 }}>{fmt(trip.departureTime)}</span>
                            <span style={{ opacity: 0.4, margin: '0 3px' }}>{'\u2013'}</span>
                            <span style={{ fontWeight: 400 }}>{fmt(trip.arrivalTime)}</span>
                            {trip.endStationName && (
                              <span style={{ opacity: 0.4, marginLeft: 6 }}>{'\u00b7'} {trip.endStationName}</span>
                            )}
                          </span>
                        )}
                        {showCompact && (
                          <span style={{
                            fontSize: 9,
                            fontFamily: 'ui-monospace, SFMono-Regular, "SF Mono", Consolas, monospace',
                            color: c.text,
                            opacity: 0.75,
                            fontWeight: 700,
                          }}>{fmt(trip.departureTime)}</span>
                        )}
                      </button>
                    </div>
                  )
                })}
              </div>
            </div>
          ))}

          <div style={{ borderTop: '1px solid #1c2128' }} />
        </div>
      </div>

      {selectedTrip && sp && (
        <div style={{
          borderTop: `1px solid ${sp.accent}22`,
          background: `linear-gradient(135deg, rgba(${hexToRgb(sp.accent)},0.055) 0%, #161b22 50%)`,
          position: 'relative',
          padding: '14px 20px 16px',
        }}>
          <div style={{
            position: 'absolute',
            left: 0, top: 0, bottom: 0,
            width: 3,
            background: sp.accent,
            opacity: 0.9,
          }} />

          <button
            type="button"
            className="bt-close"
            onClick={() => setSelectedTrip(null)}
            style={{
              position: 'absolute',
              right: 12,
              top: 12,
              width: 26,
              height: 26,
              border: 'none',
              background: 'transparent',
              color: '#484f58',
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              padding: 0,
            }}
          >
            <X size={13} />
          </button>

          <div style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fit, minmax(130px, 1fr))',
            gap: '12px 20px',
            paddingRight: 36,
          }}>
            {[
              {
                label: tf('block_id'),
                value: (
                  <span style={{ fontFamily: 'ui-monospace, SFMono-Regular, "SF Mono", Consolas, monospace' }}>
                    {selectedBlock?.blockId || '\u2014'}
                  </span>
                ),
              },
              {
                label: tf('direction_label'),
                value: selectedTrip.directionName || '\u2014',
              },
              {
                label: `${tf('departure')} \u2192 ${tf('arrival')}`,
                value: (
                  <span style={{
                    display: 'inline-flex',
                    alignItems: 'center',
                    gap: 4,
                    fontFamily: 'ui-monospace, SFMono-Regular, "SF Mono", Consolas, monospace',
                  }}>
                    <Clock size={11} style={{ color: sp.accent, flexShrink: 0 }} />
                    {fmt(selectedTrip.departureTime)}
                    <span style={{ color: '#2d333b', margin: '0 1px' }}>{'\u2192'}</span>
                    {fmt(selectedTrip.arrivalTime)}
                  </span>
                ),
              },
              {
                label: tf('from'),
                value: (
                  <span style={{ display: 'inline-flex', alignItems: 'center', gap: 4 }}>
                    <MapPin size={11} style={{ color: sp.accent, flexShrink: 0 }} />
                    {selectedTrip.startStationName || '\u2014'}
                  </span>
                ),
              },
              {
                label: tf('to'),
                value: (
                  <span style={{ display: 'inline-flex', alignItems: 'center', gap: 4 }}>
                    <MapPin size={11} style={{ color: sp.accent, flexShrink: 0 }} />
                    {selectedTrip.endStationName || '\u2014'}
                  </span>
                ),
              },
              {
                label: tf('receives_pax'),
                value: selectedTrip.receivesPassengers
                  ? <span style={{ color: '#34d399' }}>{tf('yes')}</span>
                  : <span style={{ color: '#f87171' }}>{tf('no')}</span>,
              },
            ].map(({ label, value }) => (
              <div key={label}>
                <span style={{
                  display: 'block',
                  fontSize: 9.5,
                  fontWeight: 700,
                  letterSpacing: '0.09em',
                  textTransform: 'uppercase',
                  color: '#484f58',
                  marginBottom: 4,
                }}>
                  {label}
                </span>
                <span style={{
                  fontSize: 12.5,
                  fontWeight: 500,
                  color: '#adbac7',
                  display: 'block',
                }}>
                  {value ?? '\u2014'}
                </span>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  )
}

export default BlockTimeline
