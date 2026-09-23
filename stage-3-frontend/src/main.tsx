// ============================================================================
// ENTRY POINT — the provider stack (Week 2 React: composition at the root).
// ============================================================================
// Order matters and tells a story, outside-in:
//   QueryClientProvider  — server-state cache (react-query)
//     BrowserRouter      — URL routing
//       AuthProvider     — session state (uses neither of the above, but its
//                          logout() is invoked BY the api client)
//         App            — the route table
//
// Any component inside can useQuery(), useNavigate(), useAuth() — context
// providers are how libraries "inject" capabilities down the tree.

import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { BrowserRouter } from 'react-router-dom'
import App from './App.tsx'
import { AuthProvider } from './auth/AuthProvider.tsx'
import './index.css'

// One QueryClient for the app: default staleness/refetch policy lives here.
// staleTime 30s = data younger than 30s is NOT refetched on remount (fewer
// duplicate GETs when navigating between pages); mutations invalidate
// explicitly, so freshness after writes is guaranteed regardless.
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30_000,
      retry: 1, // one retry for flaky networks; don't hammer a failing API
    },
  },
})

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    {/* StrictMode double-invokes renders/effects IN DEVELOPMENT to surface
        impure components — harmless, and it catches real bugs early. */}
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <AuthProvider>
          <App />
        </AuthProvider>
      </BrowserRouter>
    </QueryClientProvider>
  </StrictMode>,
)
