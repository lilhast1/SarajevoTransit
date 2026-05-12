# Frontend SPA Implementation Guide

## Overview

This frontend implements a **Single Page Application (SPA)** for the Sarajevo Transit system, demonstrating proper integration with backend microservices, security best practices, and modern SPA principles.

## Architecture

### Tech Stack
- **React 19** - UI framework
- **React Router v6** - Client-side routing (SPA)
- **Tailwind CSS** - Styling
- **Leaflet** - Maps
- **Vite** - Build tool

### Project Structure

```
src/
├── pages/              # Route pages (routed components)
│   ├── RoutePlannerPage.jsx      # Main route search
│   ├── LinesPage.jsx             # Lines discovery
│   ├── LineDetailPage.jsx        # Line detail view
│   ├── StopsPage.jsx             # Stops discovery
│   ├── StopDetailPage.jsx        # Stop detail view
│   ├── TimetablePage.jsx         # Timetable browser
│   ├── AuthPage.jsx              # Login/Register
│   └── ProfilePage.jsx           # User profile
├── components/
│   ├── layout/                   # Page layouts
│   ├── common/                   # Reusable components
│   │   ├── PanelCard.jsx         # Card wrapper
│   │   ├── Alerts.jsx            # Error/Success alerts
│   │   ├── LoadingStates.jsx     # Loading indicators
│   │   └── LineBadge.jsx         # Badge component
│   └── map/                      # Map components
├── services/
│   ├── gatewayClient.js          # HTTP client with JWT support
│   └── transitApi.js             # API layer (with mock fallback)
├── context/
│   └── AppContext.jsx            # Global state management
├── hooks/
│   └── useAuth.jsx               # Auth hooks
├── utils/
│   ├── authStorage.js            # Token persistence
│   ├── apiErrors.js              # Error handling
│   └── formatters.js             # Utilities
└── App.jsx                       # Route configuration
```

## Core Features

### 1. SPA Principles

**No Full Page Reloads**
- All navigation happens client-side via React Router
- Data fetches are partial (JSON only), not full HTML
- Smooth transitions between pages
- Browser back/forward buttons work correctly

**Example: Navigating to a line detail**
```javascript
// User clicks "Details" on a line
// → URL changes to /lines/5
// → Component mounts and loads data for line 5
// → No server-side HTML rendering
// → Instant navigation within the app
```

### 2. Authentication & Security

**JWT Token Management**
- Tokens stored in localStorage with expiration metadata
- Automatically included in all API requests via `Authorization: Bearer` header
- Protected routes redirect to `/auth` if user is not authenticated
- Logout clears token from storage and context

**Implementation:**

```javascript
// utils/authStorage.js - Token persistence
export function getAccessToken() {
  const session = getAuthSession()
  return session?.accessToken || null
}

// services/gatewayClient.js - Auto-inject token
async function request(path, options = {}) {
  const token = getAccessToken()
  const headers = {
    'Content-Type': 'application/json',
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...(options.headers || {}),
  }
  // ... rest of request
}

// hooks/useAuth.jsx - Protected routes
export function ProtectedRoute({ children }) {
  const { isAuthenticated } = useAuth()
  if (!isAuthenticated) {
    return <Navigate to="/auth" replace />
  }
  return children
}
```

### 3. API Integration Pattern

**Try-Then-Fallback Approach**
- Attempts to call real backend first
- Falls back to mock data if backend unavailable
- Provides seamless development experience

```javascript
// services/transitApi.js
async getLines({ search = '', vehicleType = '', activeOnly = true }) {
  try {
    // Try real backend
    const response = await gatewayClient.getLines(query)
    return filterLinesByQuery(response, search, vehicleType)
  } catch {
    // Fall back to mock data
    return filterLinesByQuery(mockLines, search, vehicleType)
  }
}
```

### 4. Error Handling & UX

**Three-Tier Error Handling**

1. **Network Errors** - Connection issues
2. **Auth Errors** - 401/403 status
3. **Server Errors** - 5xx status
4. **Client Errors** - 4xx status (user input)

**UI Components for States:**
- `LoadingSpinner` - Loading indicator
- `LoadingSkeletons` - Animated placeholders
- `ErrorAlert` - Error messages with dismiss
- `EmptyState` - No data scenarios

**Enhanced Page Example (LinesPage):**
```javascript
export function LinesPage() {
  const [lines, setLines] = useState([])
  const [error, setError] = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const fetchLines = async () => {
      try {
        const response = await transitApi.getLines(...)
        setLines(response)
      } catch (err) {
        setError(err.message)
      } finally {
        setLoading(false)
      }
    }
    fetchLines()
  }, [])

  if (loading) return <LoadingSkeletons count={3} />
  if (error) return <ErrorAlert error={error} onDismiss={() => setError(null)} />
  if (lines.length === 0) return <EmptyState title="No lines found" />
  
  return <LinesList lines={lines} />
}
```

### 5. Client-Side State Management

**Global State (AppContext)**
- Authentication status & user data
- Theme (light/dark)
- Favorites (lines & stops)
- Trip history

**Local State**
- Page-specific data (filters, search queries)
- Loading states
- Errors

## Core Pages

### 1. Route Planner (`/`)
- **Primary entry point** for transit planning
- Search departure/destination points
- **Interactive map** with click-to-select locations
- Display multiple route options
- Track trip history
- **Demonstrates:** Map integration, complex state management

### 2. Lines Discovery (`/lines`)
- Browse all active transit lines
- Real-time search and filtering by:
  - Line number/name
  - Vehicle type (tram, bus, trolleybus, minibus)
- Click to view line details
- **Demonstrates:** Client-side filtering, pagination pattern

### 3. Line Details (`/lines/:id`)
- Full line information
- Multiple directions (routes)
- Stop sequence with travel times
- **Map visualization** of route
- Add/remove from favorites
- **Demonstrates:** URL parameters, dynamic content, favorites system

### 4. Stops Discovery (`/stops`)
- Search all transit stops
- Real-time search
- Click to view stop details
- **Demonstrates:** Search optimization, pagination

### 5. Stop Details (`/stops/:id`)
- Stop location and address
- Lines serving this stop
- Next departures
- Add/remove from favorites
- **Demonstrates:** Dynamic data enrichment, joined data

### 6. Timetable (`/timetable`)
- Browse timetables by:
  - Line selection
  - Direction selection
  - Day type (weekday/saturday/sunday)
- Shows all departures for selection
- **Demonstrates:** Multi-level filtering, complex UI state

### 7. Authentication (`/auth`)
- Login/Register toggle
- Form validation
- Error messages
- Automatic redirect to profile on success
- **Demonstrates:** Form handling, error display, routing

### 8. Profile (`/profile`) - **Protected Route**
- User information display
- Favorite lines & stops
- Trip history
- Logout button
- **Demonstrates:** Protected routes, user data, history tracking

## API Integration

### Gateway Client Methods

```javascript
// Lines
getLines(query)           // GET /api/v1/lines
getLineById(lineId)       // GET /api/v1/lines/{id}

// Directions
getDirections(query)      // GET /api/v1/directions
getDirectionStations(id)  // GET /api/v1/directions/{id}/stations
getDirectionGeoJson(id)   // GET /api/v1/directions/{id}/geojson

// Stations/Stops
getStations(query)        // GET /api/v1/stations
getStationById(id)        // GET /api/v1/stations/{id}

// Timetables
getTimetables(query)      // GET /api/v1/timetables

// Route Planning
getOptimalRoute(query)    // GET /api/v1/routes/optimal

// Authentication
register(data)            // POST /api/v1/auth/register
login(data)              // POST /api/v1/auth/login
logout()                 // POST /api/v1/auth/logout
getCurrentUser()         // GET /api/v1/users/me
```

### Request/Response Flow

```
User Action
    ↓
Component Event Handler
    ↓
API Call (transitApi/gatewayClient)
    ↓
JWT Token Auto-Injected (if authenticated)
    ↓
HTTP Request to Gateway
    ↓
Backend Response
    ↓
Try/Catch → Mock Fallback if Error
    ↓
Update Local State
    ↓
Component Re-render
    ↓
User Sees Updated UI (no page reload!)
```

## Security Features

### 1. JWT Authentication
- **Token Storage:** localStorage (with expiration tracking)
- **Token Injection:** Automatic in all API requests
- **Token Expiration:** Client-side tracking

### 2. Protected Routes
```javascript
// Profile route is protected
<Route
  path="/profile"
  element={
    <ProtectedRoute>
      <ProfilePage />
    </ProtectedRoute>
  }
/>
```

### 3. Authorization Headers
```javascript
// All requests include Bearer token
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### 4. Logout Flow
- Clear token from storage
- Update authentication context
- Redirect to `/auth`
- All subsequent requests have no token

## Development Patterns

### Data Loading Pattern
```javascript
useEffect(() => {
  let active = true
  setLoading(true)
  setError(null)

  const loadData = async () => {
    try {
      const data = await fetchData()
      if (active) setData(data)
    } catch (err) {
      if (active) setError(err)
    } finally {
      if (active) setLoading(false)
    }
  }

  loadData()
  return () => { active = false }
}, [/* dependencies */])
```

### Search Filtering Pattern
```javascript
const filteredItems = useMemo(
  () => items.filter(item => 
    item.name.includes(query) &&
    (!typeFilter || item.type === typeFilter)
  ),
  [items, query, typeFilter]
)
```

### Favorites Pattern
```javascript
const isFavorited = useMemo(
  () => favorites.includes(itemId),
  [favorites, itemId]
)

const toggleFavorite = () => {
  if (isFavorited) {
    removeFavorite(itemId)
  } else {
    addFavorite(itemId)
  }
}
```

## Running the Application

### Development
```bash
cd frontend
npm install
npm run dev
```

The app runs on `http://localhost:5173` and proxies `/api` requests to `http://localhost:8080`.

### Production Build
```bash
npm run build
npm run preview
```

## Browser Support

- Modern browsers with ES6+ support
- Requires localStorage support
- Recommended: Chrome, Firefox, Safari, Edge (latest versions)

## Performance Considerations

### 1. Code Splitting
React Router automatically code-splits at route boundaries

### 2. Lazy Loading
Images and heavy components can be lazy-loaded

### 3. Caching
- Mock data is cached in memory
- API responses could be cached with React Query (Future improvement)

### 4. Optimizations
- Memoized selectors with `useMemo`
- Debounced search inputs
- Cleanup functions in useEffect to prevent memory leaks

## Testing Credentials

When backend is unavailable, use mock accounts:
- Email: `demo@sarajevotransit.ba`
- Password: `Password123`

## Future Enhancements

1. **React Query/SWR** - Better data fetching/caching
2. **Form Validation** - Zod/Yup for schemas
3. **Animations** - Framer Motion for smooth transitions
4. **Accessibility** - ARIA labels, keyboard navigation
5. **Offline Support** - Service Workers
6. **Real-time Updates** - WebSocket integration
7. **Analytics** - Track user behavior
8. **A/B Testing** - Feature flags

## Common Issues & Solutions

### Issue: Token not being sent in requests
**Solution:** Check `authStorage.js` - ensure `getAccessToken()` returns a valid token

### Issue: "Page not found" on refresh
**Solution:** This is expected for client-side routes. Server should redirect to `index.html`

### Issue: Backend returning 401 (Unauthorized)
**Solution:** Token may have expired. User needs to login again

### Issue: API calls falling back to mock data
**Solution:** Backend may be unavailable. Check network tab and backend logs

## Contributing

When adding new pages:
1. Create new `.jsx` file in `src/pages/`
2. Add route to `App.jsx`
3. Implement error handling with try/catch
4. Add loading states
5. Use `useMemo` for expensive computations
6. Clean up in useEffect return function

---

**Last Updated:** May 2026  
**Status:** Core SPA implementation complete with full authentication and error handling
