import { useEffect, useState, useCallback, useMemo, useRef } from 'react'
import { useAppContext } from '../../context/AppContext'
import {
  AreaChart, Area, BarChart, Bar, PieChart, Pie, Cell,
  LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip,
  Legend, ResponsiveContainer,
} from 'recharts'
import {
  TrendingUp, TicketIcon, AlertCircle, Star, FileText,
  Download, Table2, RefreshCw,
} from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { PanelCard } from '../../components/common/PanelCard'
import { AdminPagePanel } from '../../components/common/AdminPagePanel'
import { GraphRebuildPanel } from '../../components/admin/GraphRebuildPanel'
import { gatewayClient } from '../../services/gatewayClient'
import jsPDF from 'jspdf'
import autoTable from 'jspdf-autotable'
import * as XLSX from 'xlsx'

const PERIODS = ['TODAY', 'WEEK', 'MONTH']

const TYPE_COLORS  = { SINGLE: '#6366f1', DAILY: '#22c55e', WEEKLY: '#f59e0b', MONTHLY: '#06b6d4' }
const TYPE_ORDER   = ['SINGLE', 'DAILY', 'WEEKLY', 'MONTHLY']
const CAT_COLORS   = {
  BREAKDOWN: '#ef4444', CROWDING: '#f59e0b', HYGIENE: '#8b5cf6',
  AGGRESSIVE_BEHAVIOR: '#ec4899', DELAY: '#06b6d4', OTHER: '#6b7280',
}
const POLL_MS = 30_000

function fmt(n, decimals = 0) {
  if (n == null) return '—'
  return Number(n).toLocaleString('en-US', { minimumFractionDigits: decimals, maximumFractionDigits: decimals })
}
function fmtBAM(n) { return n == null ? '—' : `${fmt(n, 2)} BAM` }
function fmtRating(n) { return n == null ? '—' : Number(n).toFixed(1) }

function KpiCard({ icon: Icon, label, value, color, loading }) {
  return (
    <PanelCard tone="soft">
      <div className="flex items-start gap-3">
        <div className={`shrink-0 rounded-lg p-2 ${color}`}>
          <Icon size={18} aria-hidden />
        </div>
        <div className="min-w-0">
          {loading
            ? <div className="mb-1 h-7 w-24 animate-pulse rounded bg-border" />
            : <p className="text-2xl font-bold tabular-nums text-ink">{value}</p>}
          <p className="text-xs text-muted">{label}</p>
        </div>
      </div>
    </PanelCard>
  )
}

function ChartCard({ title, children, loading, minH = 220 }) {
  return (
    <PanelCard>
      <h3 className="mb-3 text-base font-semibold text-ink">{title}</h3>
      {loading
        ? <div className="animate-pulse rounded bg-border" style={{ minHeight: minH }} />
        : <div style={{ minHeight: minH }}>{children}</div>}
    </PanelCard>
  )
}

function SimpleTable({ headers, rows, loading }) {
  if (loading) return <div className="h-32 animate-pulse rounded bg-border" />
  if (!rows.length) return <p className="text-sm text-muted">No data</p>
  return (
    <div className="overflow-x-auto">
      <table className="w-full text-sm">
        <thead>
          <tr className="border-b border-border">
            {headers.map((h) => (
              <th key={h} className="pb-1.5 pr-4 text-left text-xs font-semibold text-muted last:pr-0">{h}</th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.map((r, i) => (
            <tr key={i} className="border-b border-border/50 last:border-0">
              {r.map((cell, j) => (
                <td key={j} className="py-1.5 pr-4 text-ink last:pr-0">{cell ?? '—'}</td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

export function AdminDashboardPage() {
  const { t } = useTranslation('admin-dashboard')
  const { theme } = useAppContext()

  const PERIOD_LABEL = {
    TODAY: t('period_today'),
    WEEK:  t('period_week'),
    MONTH: t('period_month'),
  }

  const [period, setPeriod]             = useState('MONTH')
  const [finance, setFinance]           = useState(null)
  const [feedback, setFeedback]         = useState(null)
  const [lineSubs, setLineSubs]         = useState(null)
  const [delays, setDelays]             = useState(null)
  const [buyerNames, setBuyerNames]     = useState({})
  const [linesMap, setLinesMap]         = useState({})
  const [loadingInit, setLoadingInit]   = useState(true)
  const [loadingFin, setLoadingFin]     = useState(false)
  const pollRef = useRef(null)

  const fetchLines = useCallback(async () => {
    try {
      const lines = await gatewayClient.getLines()
      const map = {}
      ;(Array.isArray(lines) ? lines : lines?.content ?? []).forEach((l) => {
        map[l.id] = { code: l.code, name: l.name }
      })
      setLinesMap(map)
    } catch (_) {}
  }, [])

  const resolveNames = useCallback(async (buyers) => {
    if (!buyers?.length) return
    const results = await Promise.allSettled(
      buyers.map((b) => gatewayClient.getUserById(b.userId))
    )
    const map = {}
    results.forEach((r, i) => {
      if (r.status === 'fulfilled') {
        map[buyers[i].userId] = r.value?.fullName ?? r.value?.profile?.fullName ?? `User ${buyers[i].userId}`
      }
    })
    setBuyerNames(map)
  }, [])

  const fetchFinance = useCallback(async (p) => {
    setLoadingFin(true)
    try {
      const fin = await gatewayClient.getAdminStats(p)
      setFinance(fin)
      resolveNames(fin?.topBuyers)
    } catch (_) {}
    finally { setLoadingFin(false) }
  }, [resolveNames])

  useEffect(() => {
    async function init() {
      setLoadingInit(true)
      const [finRes, fbRes, subRes, delRes] = await Promise.allSettled([
        gatewayClient.getAdminStats(period),
        gatewayClient.getAdminFeedbackStats(),
        gatewayClient.getLineSubscriptionStats(),
        gatewayClient.getTripDelays(),
      ])
      if (finRes.status === 'fulfilled') { setFinance(finRes.value); resolveNames(finRes.value?.topBuyers) }
      if (fbRes.status  === 'fulfilled') setFeedback(fbRes.value)
      if (subRes.status === 'fulfilled') setLineSubs(subRes.value)
      if (delRes.status === 'fulfilled') setDelays(delRes.value)
      setLoadingInit(false)
    }
    init()
    fetchLines()
  }, []) // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    if (!loadingInit) fetchFinance(period)
  }, [period]) // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    pollRef.current = setInterval(() => fetchFinance(period), POLL_MS)
    return () => clearInterval(pollRef.current)
  }, [period, fetchFinance])

  const tooltipStyle = useMemo(() => theme === 'dark'
    ? { backgroundColor: '#1e1e2e', border: '1px solid #374151', borderRadius: '6px', color: '#f1f5f9', fontSize: 12 }
    : { backgroundColor: '#ffffff', border: '1px solid #e5e7eb', borderRadius: '6px', color: '#0f172a', fontSize: 12 },
    [theme])

  const gridColor = theme === 'dark' ? '#374151' : '#e5e7eb'

  const revenueData = useMemo(() =>
    (finance?.revenueTimeSeries ?? []).map((p) => ({
      date: p.date ? `${p.date.slice(8)}-${p.date.slice(5, 7)}` : p.date,
      revenue: Number(p.revenue ?? 0),
      tickets: Number(p.count ?? 0),
    })), [finance])

  const typeData = useMemo(() =>
    TYPE_ORDER
      .filter((k) => (finance?.ticketsByType?.[k] ?? 0) > 0)
      .map((k) => ({ name: k.charAt(0) + k.slice(1).toLowerCase(), value: finance.ticketsByType[k], fill: TYPE_COLORS[k] })),
    [finance])

  const hourlyData = useMemo(() =>
    Array.from({ length: 24 }, (_, h) => ({
      hour: `${String(h).padStart(2, '0')}h`,
      tickets: (finance?.hourlyDistribution ?? []).find((p) => Number(p.hour) === h)?.count ?? 0,
    })), [finance])

  const ratingTrendData = useMemo(() =>
    (feedback?.ratingTrend ?? []).map((p) => ({
      month: p.month?.slice(5) ?? p.month,
      rating: Number((p.avgRating ?? 0).toFixed(2)),
      reviews: Number(p.reviewCount ?? 0),
    })), [feedback])

  const categoryData = useMemo(() =>
    Object.entries(feedback?.reportsByCategory ?? {}).map(([k, v]) => ({
      name: k.replace(/_/g, ' ').toLowerCase().replace(/^\w/, (c) => c.toUpperCase()),
      count: Number(v),
      fill: CAT_COLORS[k] ?? '#6b7280',
    })).sort((a, b) => b.count - a.count),
    [feedback])

  const { delayRows, delayChartData } = useMemo(() => {
    const todayYYYYMMDD = new Date().toISOString().slice(0, 10).replace(/-/g, '')
    const list = (Array.isArray(delays) ? delays : (delays?.delays ?? delays?.content ?? []))
      .filter((d) => !d.serviceDate || d.serviceDate === todayYYYYMMDD)
    const grouped = {}
    list.forEach((d) => {
      const key = d.lineCode ?? d.lineId
      if (!grouped[key]) grouped[key] = { lineCode: d.lineCode ?? key, lineName: d.lineName ?? '', delays: [] }
      grouped[key].delays.push(Math.abs(Number(d.delaySeconds ?? 0)))
    })
    const entries = Object.values(grouped).map((g) => {
      const avg = Math.round(g.delays.reduce((a, b) => a + b, 0) / g.delays.length / 60)
      return { lineCode: g.lineCode, lineName: g.lineName, avgMin: avg, count: g.delays.length }
    }).sort((a, b) => b.avgMin - a.avgMin)

    return {
      delayRows: entries.map((e) => [e.lineCode, e.lineName, `${e.avgMin} min`, e.count]),
      delayChartData: entries.slice(0, 8).map((e) => ({ line: e.lineCode, avgMin: e.avgMin })),
    }
  }, [delays])

  const subsRows = useMemo(() =>
    (Array.isArray(lineSubs) ? lineSubs : [])
      .slice(0, 10)
      .map((s) => [s.lineCode ?? '—', s.lineName ?? '—', fmt(s.subscriberCount)]),
    [lineSubs])

  const reportedRows = useMemo(() =>
    (feedback?.mostReportedLines ?? []).map((r) => {
      const line = linesMap[r.lineId] ?? {}
      return [line.code ?? `#${r.lineId}`, line.name ?? '—', fmt(r.count)]
    }), [feedback, linesMap])

  const buyerRows = useMemo(() =>
    (finance?.topBuyers ?? []).map((b) => [
      buyerNames[b.userId] ?? `User ${b.userId}`,
      fmt(b.ticketCount),
      fmtBAM(b.totalSpent),
    ]), [finance, buyerNames])

  const totalRevenue  = fmtBAM(finance?.totalRevenue)
  const totalTickets  = fmt(finance?.totalTickets)
  const activeTickets = fmt(finance?.activeTickets)
  const openReports   = fmt((feedback?.reportsByStatus?.RECEIVED ?? null))
  const avgRating     = fmtRating(feedback?.averageRating)
  const totalReviews  = fmt(feedback?.totalReviews)

  function exportPDF() {
    const doc = new jsPDF()
    doc.setFontSize(16)
    doc.text(t('export_title'), 14, 16)
    doc.setFontSize(10)
    doc.text(`${t('export_period')}: ${PERIOD_LABEL[period]}  |  ${t('export_generated')}: ${new Date().toLocaleString()}`, 14, 23)

    autoTable(doc, {
      startY: 30,
      head: [[t('export_metric'), t('export_value')]],
      body: [
        [t('kpi_revenue'), totalRevenue],
        [t('kpi_tickets_sold'), totalTickets],
        [t('kpi_active_tickets'), activeTickets],
        [t('kpi_open_reports'), openReports],
        [t('kpi_avg_rating'), avgRating],
        [t('kpi_total_reviews'), totalReviews],
      ],
    })

    if (buyerRows.length) {
      autoTable(doc, {
        startY: doc.lastAutoTable.finalY + 8,
        head: [['Top Buyers', 'Tickets', 'Revenue (BAM)']],
        body: buyerRows,
      })
    }

    if (delayRows.length) {
      autoTable(doc, {
        startY: doc.lastAutoTable.finalY + 8,
        head: [[t('col_line'), t('col_name'), t('col_avg_delay'), t('export_col_active_delays')]],
        body: delayRows,
      })
    }

    if (subsRows.length) {
      autoTable(doc, {
        startY: doc.lastAutoTable.finalY + 8,
        head: [[t('col_line'), t('col_name'), t('col_subscribers')]],
        body: subsRows,
      })
    }

    doc.save(`sarajevo-transit-stats-${period.toLowerCase()}.pdf`)
  }

  function exportExcel() {
    const wb = XLSX.utils.book_new()

    XLSX.utils.book_append_sheet(wb,
      XLSX.utils.aoa_to_sheet([
        [t('export_metric'), t('export_value')],
        [t('kpi_revenue') + ' (BAM)', Number(finance?.totalRevenue ?? 0)],
        [t('kpi_tickets_sold'), Number(finance?.totalTickets ?? 0)],
        [t('kpi_active_tickets'), Number(finance?.activeTickets ?? 0)],
        [t('kpi_open_reports'), Number(feedback?.reportsByStatus?.RECEIVED ?? 0)],
        [t('kpi_avg_rating'), Number(feedback?.averageRating ?? 0)],
        [t('kpi_total_reviews'), Number(feedback?.totalReviews ?? 0)],
      ]),
      t('export_sheet_summary')
    )

    if (revenueData.length) {
      XLSX.utils.book_append_sheet(wb,
        XLSX.utils.json_to_sheet(revenueData),
        t('export_sheet_revenue')
      )
    }

    if (buyerRows.length) {
      XLSX.utils.book_append_sheet(wb,
        XLSX.utils.aoa_to_sheet([[t('col_passenger'), t('col_tickets'), t('col_revenue') + ' (BAM)'], ...buyerRows]),
        t('export_sheet_top_buyers')
      )
    }

    if (delayRows.length) {
      XLSX.utils.book_append_sheet(wb,
        XLSX.utils.aoa_to_sheet([[t('col_line'), t('col_name'), t('col_avg_delay'), t('export_col_active_delays')], ...delayRows]),
        t('export_sheet_delays')
      )
    }

    XLSX.writeFile(wb, `sarajevo-transit-stats-${period.toLowerCase()}.xlsx`)
  }

  const isLoading = loadingInit

  return (
    <AdminPagePanel>
      <AdminPagePanel.Header
        title={t('title')}
        subtitle={t('subtitle')}
        actions={
          <div className="flex items-center gap-2">
            <div className="flex rounded-panel border border-border overflow-hidden text-sm">
              {PERIODS.map((p) => (
                <button
                  key={p}
                  onClick={() => setPeriod(p)}
                  className={`px-3 py-1.5 transition ${period === p ? 'bg-accent text-white' : 'text-muted hover:bg-surface-alt'}`}
                >
                  {PERIOD_LABEL[p]}
                </button>
              ))}
            </div>
            {loadingFin && <RefreshCw size={14} className="animate-spin text-muted" />}
            <button onClick={exportPDF}
              className="flex items-center gap-1.5 rounded-panel border border-border px-3 py-1.5 text-sm text-muted hover:bg-surface-alt transition">
              <Download size={14} /> {t('btn_pdf')}
            </button>
            <button onClick={exportExcel}
              className="flex items-center gap-1.5 rounded-panel border border-border px-3 py-1.5 text-sm text-muted hover:bg-surface-alt transition">
              <Table2 size={14} /> {t('btn_excel')}
            </button>
          </div>
        }
      />

      <GraphRebuildPanel />

      <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-6">
        <KpiCard icon={TrendingUp}   label={t('kpi_revenue')}        value={totalRevenue}  color="bg-indigo-100 text-indigo-700 dark:bg-indigo-900/30 dark:text-indigo-400"  loading={isLoading || loadingFin} />
        <KpiCard icon={TicketIcon}   label={t('kpi_tickets_sold')}   value={totalTickets}  color="bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400"      loading={isLoading || loadingFin} />
        <KpiCard icon={TicketIcon}   label={t('kpi_active_tickets')} value={activeTickets} color="bg-cyan-100 text-cyan-700 dark:bg-cyan-900/30 dark:text-cyan-400"          loading={isLoading || loadingFin} />
        <KpiCard icon={AlertCircle}  label={t('kpi_open_reports')}   value={openReports}   color="bg-yellow-100 text-yellow-700 dark:bg-yellow-900/30 dark:text-yellow-400"  loading={isLoading} />
        <KpiCard icon={Star}         label={t('kpi_avg_rating')}     value={avgRating}     color="bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-400"      loading={isLoading} />
        <KpiCard icon={FileText}     label={t('kpi_total_reviews')}  value={totalReviews}  color="bg-purple-100 text-purple-700 dark:bg-purple-900/30 dark:text-purple-400"  loading={isLoading} />
      </div>

      <div className="grid gap-4 lg:grid-cols-3">
        <div className="lg:col-span-2">
          <ChartCard title={t('chart_revenue')} loading={isLoading || loadingFin}>
            <ResponsiveContainer width="100%" height={220}>
              <AreaChart data={revenueData} margin={{ top: 4, right: 8, left: 0, bottom: 0 }}>
                <defs>
                  <linearGradient id="revGrad" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%"  stopColor="#6366f1" stopOpacity={0.25} />
                    <stop offset="95%" stopColor="#6366f1" stopOpacity={0} />
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" stroke={gridColor} />
                <XAxis dataKey="date" tick={{ fontSize: 11 }} />
                <YAxis tick={{ fontSize: 11 }} width={55} tickFormatter={(v) => `${v} BAM`} />
                <Tooltip contentStyle={tooltipStyle} formatter={(v) => [`${Number(v).toFixed(2)} BAM`, t('tooltip_revenue')]} />
                <Area type="monotone" dataKey="revenue" stroke="#6366f1" fill="url(#revGrad)" strokeWidth={2} dot={false} />
              </AreaChart>
            </ResponsiveContainer>
          </ChartCard>
        </div>

        <ChartCard title={t('chart_ticket_types')} loading={isLoading || loadingFin}>
          <ResponsiveContainer width="100%" height={220}>
            <PieChart>
              <Pie data={typeData} cx="50%" cy="50%" innerRadius={55} outerRadius={80} paddingAngle={3} dataKey="value">
                {typeData.map((entry) => (
                  <Cell key={entry.name} fill={entry.fill} />
                ))}
              </Pie>
              <Tooltip contentStyle={tooltipStyle} formatter={(v, n) => [fmt(v), n]} />
              <Legend iconSize={10} wrapperStyle={{ fontSize: 11 }} />
            </PieChart>
          </ResponsiveContainer>
        </ChartCard>
      </div>

      <div className="grid gap-4 lg:grid-cols-2">
        <ChartCard title={t('chart_busiest_hours')} loading={isLoading || loadingFin}>
          <ResponsiveContainer width="100%" height={200}>
            <BarChart data={hourlyData} margin={{ top: 4, right: 8, left: 0, bottom: 0 }}>
              <CartesianGrid strokeDasharray="3 3" stroke="var(--color-border, #e5e7eb)" />
              <XAxis dataKey="hour" tick={{ fontSize: 10 }} interval={2} />
              <YAxis tick={{ fontSize: 11 }} allowDecimals={false} />
              <Tooltip contentStyle={tooltipStyle} />
              <Bar dataKey="tickets" fill="#6366f1" radius={[3, 3, 0, 0]} maxBarSize={18} />
            </BarChart>
          </ResponsiveContainer>
        </ChartCard>

        <ChartCard title={t('chart_rating_trend')} loading={isLoading}>
          <ResponsiveContainer width="100%" height={200}>
            <LineChart data={ratingTrendData} margin={{ top: 4, right: 8, left: 0, bottom: 0 }}>
              <CartesianGrid strokeDasharray="3 3" stroke="var(--color-border, #e5e7eb)" />
              <XAxis dataKey="month" tick={{ fontSize: 11 }} />
              <YAxis domain={[1, 5]} tick={{ fontSize: 11 }} ticks={[1, 2, 3, 4, 5]} />
              <Tooltip contentStyle={tooltipStyle} formatter={(v) => [Number(v).toFixed(2), t('tooltip_avg_rating')]} />
              <Line type="monotone" dataKey="rating" stroke="#f59e0b" strokeWidth={2} dot={{ r: 3 }} />
            </LineChart>
          </ResponsiveContainer>
        </ChartCard>
      </div>

      <ChartCard title={t('chart_reports_category')} loading={isLoading}>
        <ResponsiveContainer width="100%" height={180}>
          <BarChart data={categoryData} layout="vertical" margin={{ top: 4, right: 16, left: 100, bottom: 0 }}>
            <CartesianGrid strokeDasharray="3 3" stroke="var(--color-border, #e5e7eb)" horizontal={false} />
            <XAxis type="number" tick={{ fontSize: 11 }} allowDecimals={false} />
            <YAxis type="category" dataKey="name" tick={{ fontSize: 11 }} width={96} />
            <Tooltip />
            <Bar dataKey="count" radius={[0, 3, 3, 0]} maxBarSize={20}>
              {categoryData.map((entry) => (
                <Cell key={entry.name} fill={entry.fill} />
              ))}
            </Bar>
          </BarChart>
        </ResponsiveContainer>
      </ChartCard>

      <div className="grid gap-4 lg:grid-cols-2">
        <PanelCard>
          <h3 className="mb-3 text-base font-semibold text-ink">{t('section_delays')}</h3>
          <SimpleTable
            headers={[t('col_line'), t('col_name'), t('col_avg_delay'), t('col_active')]}
            rows={delayRows.slice(0, 8)}
            loading={isLoading}
          />
          {!isLoading && delayChartData.length > 0 && (
            <div className="mt-4 border-t border-border pt-3">
              <p className="mb-2 text-xs font-semibold uppercase tracking-wide text-muted">{t('chart_avg_delay_label')}</p>
              <ResponsiveContainer width="100%" height={Math.max(80, delayChartData.length * 28)}>
                <BarChart data={delayChartData} layout="vertical" margin={{ top: 0, right: 32, left: 28, bottom: 0 }}>
                  <XAxis type="number" tick={{ fontSize: 10 }} allowDecimals={false} />
                  <YAxis type="category" dataKey="line" tick={{ fontSize: 11 }} width={26} />
                  <Tooltip contentStyle={tooltipStyle} formatter={(v) => [`${v} min`, t('tooltip_avg_delay')]} />
                  <Bar dataKey="avgMin" fill="#f59e0b" radius={[0, 3, 3, 0]} maxBarSize={14} />
                </BarChart>
              </ResponsiveContainer>
            </div>
          )}
        </PanelCard>

        <PanelCard>
          <h3 className="mb-3 text-base font-semibold text-ink">{t('section_busiest_lines')}</h3>
          <SimpleTable
            headers={[t('col_line'), t('col_name'), t('col_subscribers')]}
            rows={subsRows.slice(0, 8)}
            loading={isLoading}
          />
        </PanelCard>
      </div>

      <div className="grid gap-4 lg:grid-cols-2">
        <PanelCard>
          <h3 className="mb-3 text-base font-semibold text-ink">{t('section_reported_lines')}</h3>
          <SimpleTable
            headers={[t('col_line'), t('col_name'), t('col_reports')]}
            rows={reportedRows}
            loading={isLoading}
          />
        </PanelCard>

        <PanelCard>
          <h3 className="mb-3 text-base font-semibold text-ink">{t('section_top_buyers')}</h3>
          <SimpleTable
            headers={[t('col_passenger'), t('col_tickets'), t('col_revenue')]}
            rows={buyerRows}
            loading={isLoading || loadingFin}
          />
        </PanelCard>
      </div>
    </AdminPagePanel>
  )
}

export default AdminDashboardPage
