import { useEffect, useState, useCallback } from 'react'
import { DataTable } from '../../components/admin/DataTable'
import { ErrorAlert } from '../../components/common/Alerts'
import { useAppContext } from '../../context/AppContext'
import { gatewayClient } from '../../services/gatewayClient'

export function AdminUsersPage() {
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
    if (!window.confirm('Delete this user? This action cannot be undone — all user data will be removed.')) return
    try {
      await gatewayClient.deleteUser(id)
      load()
    } catch (err) {
      setError(err.message)
    }
  }

  const columns = [
    { key: 'id', label: 'ID' },
    { key: 'fullName', label: 'Full Name' },
    { key: 'email', label: 'Email' },
    {
      key: 'role', label: 'Role', render: (r) => (
        <span className={`rounded-full px-2 py-0.5 text-xs font-medium ${
          r.role === 'ADMIN'
            ? 'bg-purple-100 text-purple-700 dark:bg-purple-900/30 dark:text-purple-400'
            : 'bg-gray-100 text-gray-600 dark:bg-gray-800 dark:text-gray-400'
        }`}>
          {r.role ?? 'USER'}
        </span>
      )
    },
    {
      key: 'createdAt', label: 'Joined', render: (r) =>
        r.createdAt ? new Date(r.createdAt).toLocaleDateString() : '—'
    },
    {
      key: 'actions', label: 'Actions', render: (r) =>
        r.id === currentUserId ? (
          <span className="rounded-full bg-gray-100 px-2 py-0.5 text-xs text-gray-400 dark:bg-gray-800">You</span>
        ) : (
          <button
            type="button"
            onClick={() => handleDelete(r.id)}
            className="rounded border border-red-300 px-2 py-0.5 text-xs text-red-600 hover:bg-red-50 dark:hover:bg-red-950"
          >
            Delete
          </button>
        )
    },
  ]

  return (
    <div className="space-y-4">
      <div>
        <h2 className="text-xl font-semibold text-ink">Users</h2>
        <p className="mt-1 text-sm text-muted">View and manage registered users.</p>
      </div>

      <ErrorAlert error={error} onDismiss={() => setError(null)} />

      <DataTable
        columns={columns}
        rows={data.content ?? []}
        page={page}
        totalPages={data.totalPages ?? 0}
        onPageChange={setPage}
        loading={loading}
      />
    </div>
  )
}

export default AdminUsersPage
