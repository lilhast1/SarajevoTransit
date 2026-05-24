import { useEffect, useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { PanelCard } from '../components/common/PanelCard'
import { transitApi } from '../services/transitApi'

export function TimetablePage() {
  const { t } = useTranslation('timetable')
  const dayTypes = [
    { id: 'weekday', label: t('weekday') },
    { id: 'saturday', label: t('saturday') },
    { id: 'sunday', label: t('sunday') },
  ]
  const [lines, setLines] = useState([])
  const [directions, setDirections] = useState([])
  const [lineId, setLineId] = useState('')
  const [directionId, setDirectionId] = useState('')
  const [dayType, setDayType] = useState('weekday')
  const [rows, setRows] = useState([])

  useEffect(() => {
    transitApi.getLines({ activeOnly: true }).then((response) => {
      setLines(response)
      if (response[0]) setLineId(String(response[0].id))
    })
  }, [])

  useEffect(() => {
    if (!lineId) return
    transitApi.getDirectionsByLine(lineId).then((response) => {
      setDirections(response)
      if (response[0]) setDirectionId(String(response[0].id))
    })
  }, [lineId])

  useEffect(() => {
    if (!lineId || !directionId) return
    transitApi.getTimetable({ lineId, directionId, dayType }).then(setRows)
  }, [dayType, directionId, lineId])

  const groupedByHour = useMemo(() => {
    const map = new Map()
    rows.forEach((row) => {
      const [hour, minute] = row.departureTime.split(':')
      if (!map.has(hour)) map.set(hour, [])
      map.get(hour).push(minute)
    })
    return Array.from(map.entries())
  }, [rows])

  return (
    <div className="space-y-4">
      <PanelCard tone="soft">
        <h2 className="text-xl font-semibold text-ink">{t('title')}</h2>
        <p className="mt-1 text-sm text-muted">{t('subtitle')}</p>

        <div className="mt-4 grid gap-3 md:grid-cols-3">
          <label htmlFor="timetable-line" className="sr-only">
            {t('select_line')}
          </label>
          <select
            id="timetable-line"
            value={lineId}
            onChange={(event) => setLineId(event.target.value)}
            className="rounded-lg border border-border bg-surface px-3 py-2 text-sm text-ink"
          >
            {lines.map((line) => (
              <option key={line.id} value={line.id}>
                {line.code} · {line.name}
              </option>
            ))}
          </select>

          <label htmlFor="timetable-direction" className="sr-only">
            {t('select_direction')}
          </label>
          <select
            id="timetable-direction"
            value={directionId}
            onChange={(event) => setDirectionId(event.target.value)}
            className="rounded-lg border border-border bg-surface px-3 py-2 text-sm text-ink"
          >
            {directions.map((direction) => (
              <option key={direction.id} value={direction.id}>
                {direction.directionLabel} · {direction.name}
              </option>
            ))}
          </select>

          <label htmlFor="timetable-daytype" className="sr-only">
            {t('select_day')}
          </label>
          <select
            id="timetable-daytype"
            value={dayType}
            onChange={(event) => setDayType(event.target.value)}
            className="rounded-lg border border-border bg-surface px-3 py-2 text-sm text-ink"
          >
            {dayTypes.map((item) => (
              <option key={item.id} value={item.id}>
                {item.label}
              </option>
            ))}
          </select>
        </div>
      </PanelCard>

      <PanelCard>
        <h3 className="text-base font-semibold text-ink">{t('departure_grid')}</h3>

        {groupedByHour.length === 0 ? (
          <p className="mt-3 text-sm text-muted">{t('no_departures')}</p>
        ) : (
          <div className="mt-3 grid gap-2">
            {groupedByHour.map(([hour, minutes]) => (
              <div
                key={hour}
                className="grid gap-2 rounded-lg border border-border bg-surface-soft px-3 py-2 md:grid-cols-[64px_1fr]"
              >
                <p className="text-sm font-semibold text-ink">{hour}:00</p>
                <p className="text-sm text-muted">{minutes.join(' · ')}</p>
              </div>
            ))}
          </div>
        )}
      </PanelCard>
    </div>
  )
}
