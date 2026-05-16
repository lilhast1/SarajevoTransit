import { prettyVehicleType } from '../../utils/formatters'
import { getVehicleTypeMetaByName, withAlpha } from '../../constants/vehicleColors'

export function LineBadge({ line }) {
  const type = line.vehicleTypeName || 'bus'
  const meta = getVehicleTypeMetaByName(type) || getVehicleTypeMetaByName('bus')
  const style = {
    backgroundColor: withAlpha(meta.color, 0.16),
    color: meta.color,
    borderColor: withAlpha(meta.color, 0.38),
  }

  return (
    <span className="inline-flex items-center rounded-full border px-3 py-1 text-xs font-semibold" style={style}>
      {line.code} · {prettyVehicleType(type)}
    </span>
  )
}
