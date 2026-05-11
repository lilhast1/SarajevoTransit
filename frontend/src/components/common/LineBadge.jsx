import { prettyVehicleType } from '../../utils/formatters'

const typeStyle = {
  tram: 'bg-red-100 text-red-800 dark:bg-red-900/50 dark:text-red-100',
  bus: 'bg-blue-100 text-blue-800 dark:bg-blue-900/50 dark:text-blue-100',
  trolleybus: 'bg-emerald-100 text-emerald-800 dark:bg-emerald-900/50 dark:text-emerald-100',
  minibus: 'bg-amber-100 text-amber-800 dark:bg-amber-900/50 dark:text-amber-100',
}

export function LineBadge({ line }) {
  const type = line.vehicleTypeName || 'bus'
  const style = typeStyle[type] || typeStyle.bus
  return (
    <span className={`inline-flex items-center rounded-full px-3 py-1 text-xs font-semibold ${style}`}>
      {line.code} · {prettyVehicleType(type)}
    </span>
  )
}
