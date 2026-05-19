#!/usr/bin/env node
// Fake vehicle position simulator for SarajevoTransit.
// Seeds the vehicleservice with demo vehicles (if not already present),
// then pushes interpolated GPS positions along Sarajevo routes every 5 s.
//
// Requirements: Node.js 18+ (uses native fetch). No npm deps.
// Usage: node scripts/vehicle-mock.js
//        GATEWAY_URL=http://localhost:8080 node scripts/vehicle-mock.js

const GATEWAY_URL = process.env.GATEWAY_URL || 'http://localhost:8080'
const INTERVAL_MS = 5000

// Sarajevo route waypoints — mirror of frontend/src/data/mockTransitData.js routePolylines
const ROUTES = {
  TRAM: [
    [43.8299, 18.3048], // Ilidža Terminal
    [43.8391, 18.3240],
    [43.8452, 18.3356], // Stup
    [43.8488, 18.3697], // Otoka
    [43.8544, 18.4147], // Skenderija
    [43.8596, 18.4333], // Baščaršija
  ],
  BUS: [
    [43.8244, 18.3431], // Dobrinja
    [43.8399, 18.3582],
    [43.8488, 18.3697], // Otoka
    [43.8544, 18.4147], // Skenderija
    [43.8592, 18.4345], // Vijećnica
  ],
  TROLLEY: [
    [43.8497, 18.4211], // Trg Austrije
    [43.8544, 18.4147], // Skenderija
    [43.8488, 18.3697], // Otoka
    [43.8399, 18.3582],
    [43.8244, 18.3431], // Dobrinja
  ],
  MINIBUS: [
    [43.8550, 18.4210], // Drvenija approx
    [43.8565, 18.4255],
    [43.8580, 18.4300],
    [43.8610, 18.4380], // Sedrenik approx
  ],
}

// Mock fleet definition: registrationNumber must be unique in vehicleservice
const MOCK_FLEET = [
  { reg: 'MOCK-T-001', internalId: '301', type: 'TRAM',    capacity: 150, route: 'TRAM',    offset: 0.00 },
  { reg: 'MOCK-T-002', internalId: '302', type: 'TRAM',    capacity: 150, route: 'TRAM',    offset: 0.33 },
  { reg: 'MOCK-T-003', internalId: '303', type: 'TRAM',    capacity: 150, route: 'TRAM',    offset: 0.67 },
  { reg: 'MOCK-B-001', internalId: '101', type: 'BUS',     capacity: 80,  route: 'BUS',     offset: 0.00 },
  { reg: 'MOCK-B-002', internalId: '102', type: 'BUS',     capacity: 80,  route: 'BUS',     offset: 0.50 },
  { reg: 'MOCK-TR-001', internalId: '201', type: 'TROLLEY', capacity: 100, route: 'TROLLEY', offset: 0.00 },
  { reg: 'MOCK-TR-002', internalId: '202', type: 'TROLLEY', capacity: 100, route: 'TROLLEY', offset: 0.50 },
  { reg: 'MOCK-MB-001', internalId: '401', type: 'MINIBUS', capacity: 20,  route: 'MINIBUS', offset: 0.00 },
]

// ── helpers ────────────────────────────────────────────────────────────────────

function timestamp() {
  return new Date().toTimeString().slice(0, 8)
}

function jitter() {
  // ±0.00012 degrees ≈ ±13 m — realistic GPS noise
  return (Math.random() - 0.5) * 0.00024
}

function interpolate(waypoints, progress) {
  // progress: 0..1 along the total polyline
  if (waypoints.length === 1) return waypoints[0]
  const segCount = waypoints.length - 1
  const scaled = progress * segCount
  const segIndex = Math.min(Math.floor(scaled), segCount - 1)
  const t = scaled - segIndex
  const [lat1, lon1] = waypoints[segIndex]
  const [lat2, lon2] = waypoints[segIndex + 1]
  return [lat1 + (lat2 - lat1) * t, lon1 + (lon2 - lon1) * t]
}

async function apiGet(path) {
  const res = await fetch(`${GATEWAY_URL}${path}`, {
    headers: { 'Content-Type': 'application/json' },
  })
  if (!res.ok) throw new Error(`GET ${path} → ${res.status}`)
  return res.json()
}

async function apiPost(path, body) {
  const res = await fetch(`${GATEWAY_URL}${path}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  })
  if (!res.ok) {
    const text = await res.text().catch(() => '')
    throw new Error(`POST ${path} → ${res.status} ${text.slice(0, 120)}`)
  }
  if (res.status === 204) return null
  return res.json()
}

// ── vehicle seeding ────────────────────────────────────────────────────────────

async function ensureVehicles() {
  console.log(`[${timestamp()}] Checking fleet at ${GATEWAY_URL}/api/vehicles …`)

  let existing = []
  try {
    const page = await apiGet('/api/vehicles?size=200')
    existing = Array.isArray(page?.content) ? page.content : Array.isArray(page) ? page : []
  } catch (err) {
    console.error(`[${timestamp()}] Could not reach vehicleservice: ${err.message}`)
    console.error(`               Make sure the full stack is running (docker-compose + vehicleservice).`)
    process.exit(1)
  }

  const existingRegs = new Set(existing.map((v) => v.registrationNumber))
  const state = []

  for (const spec of MOCK_FLEET) {
    let vehicle = existing.find((v) => v.registrationNumber === spec.reg)

    if (!vehicle) {
      try {
        vehicle = await apiPost('/api/vehicles', {
          registrationNumber: spec.reg,
          internalId: spec.internalId,
          type: spec.type,
          capacity: spec.capacity,
          status: 'OPERATIONAL',
          manufactureDate: '2020-01-01',
        })
        console.log(`[${timestamp()}] Created ${spec.type} ${spec.reg} (id=${vehicle.id})`)
      } catch (err) {
        console.error(`[${timestamp()}] Failed to create ${spec.reg}: ${err.message}`)
        continue
      }
    }

    state.push({
      vehicleId: vehicle.id,
      reg: spec.reg,
      waypoints: ROUTES[spec.route],
      progress: spec.offset,
    })
  }

  console.log(`[${timestamp()}] Fleet ready — ${state.length} vehicles tracked.`)
  return state
}

// ── main loop ─────────────────────────────────────────────────────────────────

async function main() {
  const state = await ensureVehicles()
  if (state.length === 0) {
    console.error(`[${timestamp()}] No vehicles to track. Exiting.`)
    process.exit(1)
  }

  let failCount = 0

  async function tick() {
    let pushed = 0
    for (const v of state) {
      v.progress = (v.progress + 0.015) % 1
      const [lat, lon] = interpolate(v.waypoints, v.progress)
      try {
        await apiPost(`/api/vehicles/${v.vehicleId}/location`, {
          latitude: lat + jitter(),
          longitude: lon + jitter(),
        })
        pushed++
      } catch (err) {
        failCount++
        if (failCount <= 5 || failCount % 20 === 0) {
          console.error(`[${timestamp()}] Position update failed for ${v.reg}: ${err.message}`)
        }
      }
    }
    if (pushed > 0) {
      console.log(`[${timestamp()}] ${pushed}/${state.length} positions pushed`)
    }
  }

  // Run first tick immediately, then on interval
  await tick()
  setInterval(tick, INTERVAL_MS)
}

main().catch((err) => {
  console.error(err)
  process.exit(1)
})
