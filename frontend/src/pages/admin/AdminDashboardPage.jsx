import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { AlertCircle, FileText, Star, Users, Clock } from 'lucide-react'
import { PanelCard } from '../../components/common/PanelCard'
import { gatewayClient } from '../../services/gatewayClient'

function StatCard({ icon: Icon, label, value, color }) {
  return (
    <PanelCard tone="soft">
      <div className="flex items-center gap-3">
        <div className={`rounded-lg p-2 ${color}`}>
          <Icon size={20} />
        </div>
        <div>
          <p className="text-2xl font-bold text-ink">{value ?? '—'}</p>
          <p className="text-xs text-muted">{label}</p>
        </div>
      </div>
    </PanelCard>
  )
}

const sections = [
  { to: '/admin/reports', label: 'Problem Reports', icon: AlertCircle },
  { to: '/admin/reviews', label: 'Review Moderation', icon: Star },
  { to: '/admin/lines', label: 'Lines', icon: FileText },
  { to: '/admin/stations', label: 'Stations', icon: FileText },
  { to: '/admin/timetables', label: 'Timetables', icon: Clock },
  { to: '/admin/users', label: 'Users', icon: Users },
  { to: '/admin/notifications', label: 'Notifications', icon: AlertCircle },
]

export function AdminDashboardPage() {
  const [stats, setStats] = useState({ open: null, inProgress: null, users: null })

  useEffect(() => {
    async function load() {
      const [openRes, inProgressRes, usersRes] = await Promise.allSettled([
        gatewayClient.getReports('?status=RECEIVED&size=1'),
        gatewayClient.getReports('?status=IN_PROGRESS&size=1'),
        gatewayClient.getAllUsers('?page=0&size=1'),
      ])
      setStats({
        open: openRes.status === 'fulfilled' ? openRes.value?.totalElements ?? 0 : '—',
        inProgress: inProgressRes.status === 'fulfilled' ? inProgressRes.value?.totalElements ?? 0 : '—',
        users: usersRes.status === 'fulfilled' ? usersRes.value?.totalElements ?? 0 : '—',
      })
    }
    load()
  }, [])

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-xl font-semibold text-ink">Admin Panel</h2>
        <p className="mt-1 text-sm text-muted">Overview of system state and quick access to management sections.</p>
      </div>

      <div className="grid gap-4 sm:grid-cols-3">
        <StatCard icon={AlertCircle} label="Open Reports" value={stats.open} color="bg-yellow-100 text-yellow-700 dark:bg-yellow-900/30 dark:text-yellow-400" />
        <StatCard icon={Clock} label="In Progress" value={stats.inProgress} color="bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400" />
        <StatCard icon={Users} label="Total Users" value={stats.users} color="bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400" />
      </div>

      <PanelCard>
        <h3 className="mb-3 text-base font-semibold text-ink">Management Sections</h3>
        <div className="grid gap-2 sm:grid-cols-2 lg:grid-cols-3">
          {sections.map(({ to, label, icon: Icon }) => (
            <Link
              key={to}
              to={to}
              className="flex items-center gap-2 rounded-panel border border-border px-3 py-2 text-sm font-medium text-ink transition hover:bg-surface-alt hover:text-accent"
            >
              <Icon size={15} />
              {label}
            </Link>
          ))}
        </div>
      </PanelCard>
    </div>
  )
}

export default AdminDashboardPage
