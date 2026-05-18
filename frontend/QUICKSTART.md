# Quick Start Guide - Frontend Development

## What Was Built?

A production-quality **Single Page Application (SPA)** for Sarajevo Transit with:
- ✅ 8 fully functional pages
- ✅ JWT authentication
- ✅ Professional error handling
- ✅ Real-time search & filtering
- ✅ Interactive maps
- ✅ Responsive design

## Getting Started (5 minutes)

### 1. Install Dependencies
```bash
cd frontend
npm install
```

### 2. Start Development Server
```bash
npm run dev
```
Opens at `http://localhost:5173`

### 3. Open in Browser
- Click "Route Planner" to search for transit routes
- Click "Lines" to browse all bus/tram lines
- Try the search - it's all client-side (instant!)
- Click on a line to see route details
- Go to "Auth" to login/register

### 4. Try Protected Features
- **Login**: `demo@sarajevotransit.ba` / `Password123`
- **Then**: Access `/profile` to see user data
- **Features**: Add favorites, view trip history, logout

## Understanding the Code

### Core Concepts

**1. Client-Side Routing** (No page refreshes)
```javascript
// App.jsx - Define all routes
<Routes>
  <Route path="/" element={<RoutePlannerPage />} />
  <Route path="/lines" element={<LinesPage />} />
  <Route path="/lines/:id" element={<LineDetailPage />} />  // Dynamic!
  <Route path="/profile" element={<ProtectedRoute><ProfilePage /></ProtectedRoute>} />
</Routes>

// User clicks "Details" → URL changes to /lines/5
// → LineDetailPage mounts with lineId = 5
// → Use to fetch data → Component renders
// NO PAGE RELOAD!
```

**2. JWT Token Security**
```javascript
// User logs in
login() → {accessToken, refreshToken, ...} 
  → stored in localStorage
  → automatically added to requests:
     Authorization: Bearer <token>

// Protected resources check token
if (!token) return 403 Forbidden

// User logs out
logout() → clear token → redirect to /auth
```

**3. Data Loading Pattern**
```javascript
const [data, setData] = useState(null)
const [loading, setLoading] = useState(true)
const [error, setError] = useState(null)

useEffect(() => {
  let active = true  // Prevent race conditions
  
  const load = async () => {
    try {
      const result = await api.fetchData()
      if (active) setData(result)  // Only update if component still mounted
    } catch (err) {
      if (active) setError(err)
    } finally {
      if (active) setLoading(false)
    }
  }
  
  load()
  
  // Cleanup function prevents memory leaks
  return () => { active = false }
}, [dependencies])
```

**4. Error Handling**
```javascript
if (loading) return <LoadingSpinner />
if (error) return <ErrorAlert error={error} onDismiss={...} />
if (items.length === 0) return <EmptyState title="No items" />
return <ItemList items={items} />
```

## Key Files to Know

| File | Purpose |
|------|---------|
| `App.jsx` | Route configuration |
| `context/AppContext.jsx` | Global state (auth, favorites, etc) |
| `services/gatewayClient.js` | HTTP client with JWT auto-injection |
| `services/transitApi.js` | API layer (business logic) |
| `hooks/useAuth.jsx` | Protected routes component |
| `utils/authStorage.js` | Token management |
| `pages/*.jsx` | Page components (routed) |
| `components/common/` | Reusable UI components |

## Common Tasks

### Add a New Page

1. Create `pages/NewPage.jsx`:
```javascript
export function NewPage() {
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  useEffect(() => {
    let active = true
    const load = async () => {
      try {
        const result = await transitApi.getData()
        if (active) setData(result)
      } catch (err) {
        if (active) setError(err)
      } finally {
        if (active) setLoading(false)
      }
    }
    load()
    return () => { active = false }
  }, [])

  if (loading) return <LoadingSpinner />
  if (error) return <ErrorAlert error={error} />
  return <div>{/* Render data */}</div>
}
```

2. Add route to `App.jsx`:
```javascript
import { NewPage } from './pages/NewPage'

<Route path="/new-page" element={<NewPage />} />
```

3. Add navigation in `AppLayout.jsx`:
```javascript
const navItems = [
  // ... existing items
  { to: '/new-page', label: 'New Page', icon: Icon },
]
```

### Make a Protected Page

```javascript
<Route
  path="/secret"
  element={
    <ProtectedRoute>
      <SecretPage />
    </ProtectedRoute>
  }
/>
```

### Add Error Handling

```javascript
const [error, setError] = useState(null)

const handleSomething = async () => {
  try {
    await api.doSomething()
  } catch (err) {
    setError(err.message)
  }
}

return <>
  {error && <ErrorAlert error={error} onDismiss={() => setError(null)} />}
  {/* rest of page */}
</>
```

### Use Global State

```javascript
import { useAppContext } from '../context/AppContext'

export function MyComponent() {
  const { 
    isAuthenticated,      // Current auth status
    session,              // User data
    favorites,            // Saved favorites
    toggleFavoriteLine,   // Add/remove favorite
    tripHistory,          // Recent trips
    addTripHistoryItem,   // Log a trip
    theme,                // Current theme
    toggleTheme,          // Switch light/dark
  } = useAppContext()

  // Use any of these...
}
```

## Debugging Tips

### 1. Check Console
```bash
Open DevTools (F12) → Console tab
Look for error messages
```

### 2. React DevTools
```bash
Install React DevTools extension
Inspect component state and props
```

### 3. Network Tab
```bash
DevTools → Network tab
See actual API calls
Check request/response headers
Verify JWT token in Authorization header
```

### 4. Local Storage
```bash
DevTools → Application → Local Storage
View stored auth token
Check favorites and history
```

### 5. Simulate Offline
```bash
DevTools → Network tab → Throttling dropdown
Set to "Offline"
Try loading a page
Should fall back to mock data
```

## Testing Checklist

- [ ] Click on lines, verify URL changes and data loads
- [ ] Go back in browser, verify correct page loads
- [ ] Search for lines, verify instant filtering
- [ ] Login with demo credentials
- [ ] Verify profile page shows user data
- [ ] Add line to favorites
- [ ] Logout and verify redirect to auth
- [ ] Try to access profile without login (should redirect)
- [ ] Close backend, verify mock data still works
- [ ] Search stops, verify real-time filtering
- [ ] Click on stop details, verify lines displayed
- [ ] Toggle dark mode, verify theme persists on refresh

## API Integration Points

### Login Flow
```
POST /api/v1/auth/login
Body: { email, password }
Response: { accessToken, refreshToken, userId, ... }
→ Stored in localStorage
→ Used in subsequent requests
```

### Protected Requests
```
GET /api/v1/users/me
Headers: Authorization: Bearer <accessToken>
Response: { id, email, name, ... }
```

### Data Endpoints
```
GET /api/v1/lines              // All lines
GET /api/v1/lines/5            // Line #5
GET /api/v1/directions?lineId=5  // Directions for line 5
GET /api/v1/stations           // All stops
GET /api/v1/routes/optimal?fromLat=...&fromLon=...&toLat=...&toLon=...
```

## Performance Notes

- **Client-side filtering**: Instant, no network delay
- **Memoization**: Expensive computations cached with useMemo
- **Code splitting**: Each route gets its own chunk
- **Loading states**: Users never wait without feedback

## Troubleshooting

### "Cannot find module" error
```bash
# Make sure you're importing correctly
import { MyComponent } from '../path/to/MyComponent'
// Check that path and filename match exactly
```

### Blank page / Nothing renders
```bash
Check browser console for errors (F12)
Check that App.jsx has at least one route
Verify BrowserRouter is in main.jsx
```

### API calls not reaching backend
```bash
Check vite.config.js proxy configuration
Make sure /api prefix matches your routes
Verify backend is running on localhost:8080
```

### Token not being sent in requests
```bash
Check localStorage for 'sarajevo-transit-auth' key
Verify authStorage.js getAccessToken() is called
Check Network tab to see actual headers
```

### Styling not applied
```bash
Verify Tailwind CSS is imported in index.css
Check class names are correct
Build cache may be stale: npm run build
```

## Next Steps

1. **Understand the architecture** - Read `SPA_IMPLEMENTATION.md`
2. **Review a page** - Look at `pages/LineDetailPage.jsx` for patterns
3. **Add a feature** - Create a new page following the pattern
4. **Connect backend** - Ensure UserService and RoutingService are running
5. **Deploy** - Build and serve `dist/` folder

## Resources

- [React Documentation](https://react.dev/)
- [React Router](https://reactrouter.com/)
- [Tailwind CSS](https://tailwindcss.com/)
- [Leaflet Maps](https://leafletjs.com/)
- [JWT Explanation](https://jwt.io/introduction)

## Questions?

Refer to the documentation files:
- `SPA_IMPLEMENTATION.md` - Technical deep dive
- `IMPLEMENTATION_SUMMARY.md` - Feature overview
- Check comments in source code
- Look at similar pages for patterns

Happy coding! 🚀
