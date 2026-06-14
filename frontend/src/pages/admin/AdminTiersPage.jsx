import { useCallback, useEffect, useState } from 'react'
import { Award, Check, X } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { PanelCard } from '../../components/common/PanelCard'
import { ErrorAlert, SuccessAlert } from '../../components/common/Alerts'
import { gatewayClient } from '../../services/gatewayClient'

const FIELDS = [
  { key: 'tierName', label: 'Tier', type: 'text', readOnly: true },
  { key: 'minimumLifetimePoints', label: 'Min lifetime points', type: 'number' },
  { key: 'discountPercent', label: 'Discount %', type: 'number' },
  { key: 'freeRideEligible', label: 'Free ride eligible', type: 'checkbox' },
  { key: 'couponCostDiscount', label: 'Coupon cost (discount)', type: 'number' },
  { key: 'couponCostFreeRide', label: 'Coupon cost (free ride)', type: 'number' },
  { key: 'sortOrder', label: 'Sort order', type: 'number' },
]

export function AdminTiersPage() {
  const { t } = useTranslation('admin-dashboard')
  const [tiers, setTiers] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [editingId, setEditingId] = useState(null)
  const [editForm, setEditForm] = useState({})
  const [saving, setSaving] = useState(false)
  const [successMsg, setSuccessMsg] = useState(null)

  const loadTiers = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const data = await gatewayClient.getLoyaltyTierConfigs()
      setTiers(data)
    } catch (err) {
      setError(err.message || 'Failed to load tier configs')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    loadTiers()
  }, [loadTiers])

  function startEdit(tier) {
    setEditingId(tier.id)
    setEditForm({ ...tier })
    setSuccessMsg(null)
  }

  function cancelEdit() {
    setEditingId(null)
    setEditForm({})
  }

  function handleFieldChange(key, value) {
    setEditForm((prev) => ({
      ...prev,
      [key]: key === 'freeRideEligible' ? value : (value === '' ? '' : Number(value)),
    }))
  }

  async function handleSave(id) {
    setSaving(true)
    setError(null)
    setSuccessMsg(null)
    try {
      const payload = {
        tierName: editForm.tierName,
        minimumLifetimePoints: Number(editForm.minimumLifetimePoints),
        discountPercent: Number(editForm.discountPercent),
        freeRideEligible: Boolean(editForm.freeRideEligible),
        couponCostDiscount: Number(editForm.couponCostDiscount),
        couponCostFreeRide: editForm.couponCostFreeRide === '' || editForm.couponCostFreeRide === null
          ? null : Number(editForm.couponCostFreeRide),
        sortOrder: Number(editForm.sortOrder),
      }
      const updated = await gatewayClient.updateLoyaltyTierConfig(id, payload)
      setTiers((prev) => prev.map((t) => (t.id === id ? updated : t)))
      setEditingId(null)
      setSuccessMsg(`Tier "${updated.tierName}" updated successfully`)
    } catch (err) {
      setError(err.message || 'Failed to update tier config')
    } finally {
      setSaving(false)
    }
  }

  if (loading) {
    return (
      <PanelCard tone="default">
        <p className="text-sm text-muted">Loading tier configurations...</p>
      </PanelCard>
    )
  }

  return (
    <div className="space-y-4">
      <PanelCard tone="default">
        <div className="flex items-center gap-2">
          <Award size={18} className="text-accent" aria-hidden="true" />
          <h2 className="text-base font-semibold text-ink">Loyalty Tier Configuration</h2>
        </div>
        <p className="mt-1 text-sm text-muted">
          Adjust the point thresholds, discounts, and coupon costs for each loyalty tier. Changes take effect immediately.
        </p>
      </PanelCard>

      {error && (
        <ErrorAlert error={error} onDismiss={() => setError(null)} />
      )}
      {successMsg && (
        <SuccessAlert message={successMsg} onDismiss={() => setSuccessMsg(null)} />
      )}

      <div className="overflow-x-auto rounded-lg border border-border">
        <table className="w-full text-sm">
          <thead>
            <tr className="bg-surface-alt text-left text-xs font-semibold uppercase tracking-[0.1em] text-muted">
              {FIELDS.map((field) => (
                <th key={field.key} className="px-3 py-2.5 whitespace-nowrap">{field.label}</th>
              ))}
              <th className="px-3 py-2.5 whitespace-nowrap">Actions</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-border">
            {tiers.map((tier) => {
              const isEditing = editingId === tier.id
              return (
                <tr key={tier.id} className="hover:bg-surface-soft/50 transition">
                  {FIELDS.map((field) => (
                    <td key={field.key} className="px-3 py-2.5 whitespace-nowrap">
                      {isEditing ? (
                        field.readOnly ? (
                          <span className="font-medium text-ink">{tier[field.key]}</span>
                        ) : field.type === 'checkbox' ? (
                          <input
                            type="checkbox"
                            checked={Boolean(editForm[field.key])}
                            onChange={(e) => handleFieldChange(field.key, e.target.checked)}
                            className="rounded border-border text-accent focus:ring-accent/30"
                          />
                        ) : (
                          <input
                            type="number"
                            value={editForm[field.key] ?? ''}
                            onChange={(e) => handleFieldChange(field.key, e.target.value)}
                            className="w-20 rounded border border-border bg-surface px-2 py-1 text-sm text-ink outline-none ring-accent/30 focus:ring"
                          />
                        )
                      ) : (
                        <span className="text-ink">
                          {field.type === 'checkbox'
                            ? tier[field.key]
                              ? 'Yes'
                              : 'No'
                            : field.key === 'couponCostFreeRide' && tier[field.key] === null
                              ? '—'
                              : tier[field.key]}
                        </span>
                      )}
                    </td>
                  ))}
                  <td className="px-3 py-2.5 whitespace-nowrap">
                    {isEditing ? (
                      <div className="flex items-center gap-2">
                        <button
                          onClick={() => handleSave(tier.id)}
                          disabled={saving}
                          className="inline-flex items-center gap-1 rounded bg-accent px-2.5 py-1 text-xs font-semibold text-white transition hover:bg-accent/90 disabled:opacity-70"
                        >
                          <Check size={14} />
                          Save
                        </button>
                        <button
                          onClick={cancelEdit}
                          disabled={saving}
                          className="inline-flex items-center gap-1 rounded bg-surface-alt px-2.5 py-1 text-xs font-semibold text-muted transition hover:text-ink disabled:opacity-70"
                        >
                          <X size={14} />
                          Cancel
                        </button>
                      </div>
                    ) : (
                      <button
                        onClick={() => startEdit(tier)}
                        className="text-xs font-semibold text-accent underline-offset-2 hover:underline"
                      >
                        Edit
                      </button>
                    )}
                  </td>
                </tr>
              )
            })}
          </tbody>
        </table>
      </div>
    </div>
  )
}
