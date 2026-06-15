import { useEffect, useState, useCallback } from 'react'
import { useTranslation } from 'react-i18next'
import { DataTable } from '../../components/admin/DataTable'
import { AdminPagePanel } from '../../components/common/AdminPagePanel'
import { StatusBadge } from '../../components/common/StatusBadge'
import { ErrorAlert } from '../../components/common/Alerts'
import { useAppContext } from '../../context/AppContext'
import { gatewayClient } from '../../services/gatewayClient'

export function AdminUsersPage() {
  const { t } = useTranslation('admin-users')
  const { session } = useAppContext()
  const currentUserId = session?.userId

  const [page, setPage] = useState(0)
  const [data, setData] = useState({ content: [], totalPages: 0 })
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)

  const load = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const res = await gatewayClient.getAllUsers(`?page=${page}&size=20&sort=id,asc`)
      setData(res)
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }, [page])

  useEffect(() => { load() }, [load])

  async function handleDelete(id) {
    if (!window.confirm(t('delete_confirm'))) return
    try {
      await gatewayClient.deleteUser(id)
      load()
    } catch (err) {
      setError(err.message)
    }
  }

  const columns = [
    { key: 'id', label: t('col_id') },
    { key: 'fullName', label: t('col_name') },
    { key: 'email', label: t('col_email') },
    {
      key: 'role', label: t('col_role'), render: (r) => (
        <StatusBadge status={r.role === 'ADMIN' ? 'IN_PROGRESS' : 'GENERAL'} label={r.role ?? 'USER'} />
      )
    },
    {
      key: 'createdAt', label: t('col_joined'), render: (r) =>
        r.createdAt ? new Date(r.createdAt).toLocaleDateString() : '—'
    },
    {
      key: 'actions', label: t('col_actions'), render: (r) =>
        r.id === currentUserId ? (
          <span className="rounded-full bg-surface-alt px-2 py-0.5 text-xs text-muted">{t('you')}</span>
        ) : (
          <button
            type="button"
            onClick={() => handleDelete(r.id)}
            className="rounded-panel border border-danger-soft px-2.5 py-1 text-xs font-medium text-danger transition hover:bg-danger-soft/20"
          >
            {t('delete')}
          </button>
        )
    },
  ]

  return (
    <AdminPagePanel>
      <AdminPagePanel.Header
        title={t('title')}
        subtitle={t('subtitle')}
      />

      <ErrorAlert error={error} onDismiss={() => setError(null)} />

      <DataTable
        columns={columns}
        rows={data.content ?? []}
        page={page}
        totalPages={data.totalPages ?? 0}
        onPageChange={setPage}
        loading={loading}
      />
    </AdminPagePanel>
  )
}

export default AdminUsersPage
