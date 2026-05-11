export function formatDurationFromSeconds(seconds) {
  if (!seconds || Number.isNaN(seconds)) return 'N/A'
  const minutes = Math.round(seconds / 60)
  if (minutes < 60) return `${minutes} min`
  const hours = Math.floor(minutes / 60)
  const rest = minutes % 60
  return rest ? `${hours}h ${rest}m` : `${hours}h`
}

export function formatDepartureFromTimestamp(timestamp) {
  if (!timestamp) return '--:--'
  return new Date(timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
}

export function minutesUntil(timeString) {
  const [hour, minute] = timeString.split(':').map(Number)
  const now = new Date()
  const target = new Date(now)
  target.setHours(hour, minute, 0, 0)
  if (target < now) target.setDate(target.getDate() + 1)
  const diffMs = target.getTime() - now.getTime()
  return Math.round(diffMs / 60000)
}

export function prettyVehicleType(type) {
  if (!type) return 'line'
  return type[0].toUpperCase() + type.slice(1)
}
