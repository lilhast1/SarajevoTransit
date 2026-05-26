import { useEffect, useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { LoadingSpinner } from '../components/common/LoadingStates'
import { ErrorAlert } from '../components/common/Alerts'
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
  const [linesLoading, setLinesLoading] = useState(true)
  const [directionsLoading, setDirectionsLoading] = useState(false)
  const [timetableLoading, setTimetableLoading] = useState(false)

  useEffect(() => {
    let active = true
    setLinesLoading(true)

    transitApi.getLines({ activeOnly: true })
      .then((response) => {
        if (!active) return
        setLines(response)
        if (response[0]) setLineId(String(response[0].id))
      })
      .finally(() => {
        if (active) setLinesLoading(false)
      })

    return () => { active = false }
  }, [])

  useEffect(() => {
    if (!lineId) return
    let active = true
    setDirectionsLoading(true)

    transitApi.getDirectionsByLine(lineId)
      .then((response) => {
        if (!active) return
        setDirections(response)
        if (response[0]) setDirectionId(String(response[0].id))
      })
      .finally(() => {
        if (active) setDirectionsLoading(false)
      })

    return () => { active = false }
  }, [lineId])

  useEffect(() => {
    if (!lineId || !directionId) return
    let active = true
    setTimetableLoading(true)

    transitApi.getTimetable({ lineId, directionId, dayType })
      .then((response) => {
        if (active) setRows(response)
      })
      .finally(() => {
        if (active) setTimetableLoading(false)
      })

    return () => { active = false }
  }, [dayType, directionId, lineId])
  const [error, setError] = useState(null)

  useEffect(() => {
    let active = true

    const loadLines = async () => {
      try {
        setError(null)
        const response = await transitApi.getLines({ activeOnly: true })
        if (!active) return
        setLines(response)
        if (response[0]) setLineId(String(response[0].id))
      } catch (err) {
        if (!active) return
        setError(err.message || t('load_failed'))
        setLines([])
        setLineId('')
      }
    }

    loadLines()

    return () => {
      active = false
    }
  }, [t])

  useEffect(() => {
    if (!lineId) return

    let active = true

    const loadDirections = async () => {
      try {
        setError(null)
        const response = await transitApi.getDirectionsByLine(lineId)
        if (!active) return
        setDirections(response)
        if (response[0]) setDirectionId(String(response[0].id))
        else setDirectionId('')
      } catch (err) {
        if (!active) return
        setError(err.message || t('load_failed'))
        setDirections([])
        setDirectionId('')
        setRows([])
      }
    }

    loadDirections()

    return () => {
      active = false
    }
  }, [lineId, t])

  useEffect(() => {
    if (!lineId || !directionId) return

    let active = true

    const loadTimetable = async () => {
      try {
        setError(null)
        const response = await transitApi.getTimetable({ lineId, directionId, dayType })
        if (active) setRows(response)
      } catch (err) {
        if (!active) return
        setError(err.message || t('load_failed'))
        setRows([])
      }
    }

    loadTimetable()

    return () => {
      active = false
    }
  }, [dayType, directionId, lineId, t])

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
            disabled={linesLoading}
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
            disabled={directionsLoading || linesLoading}
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
            disabled={timetableLoading}
            className="rounded-lg border border-border bg-surface px-3 py-2 text-sm text-ink"
          >
            {dayTypes.map((item) => (
              <option key={item.id} value={item.id}>
                {item.label}
              </option>
            ))}
          </select>
        </div>

        {(linesLoading || directionsLoading) ? (
          <LoadingSpinner
            label={
              linesLoading
                ? 'Loading lines...'
                : 'Loading directions...'
            }
          />
        ) : null}
      </PanelCard>

      {error ? <ErrorAlert error={error} onDismiss={() => setError(null)} /> : null}

      <PanelCard>
        <h3 className="text-base font-semibold text-ink">{t('departure_grid')}</h3>

        {timetableLoading ? (
          <LoadingSpinner label="Loading timetable..." />
        ) : groupedByHour.length === 0 ? (
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
