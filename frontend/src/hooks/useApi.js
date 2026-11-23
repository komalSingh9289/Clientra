import { useCallback, useEffect, useState } from "react";

/**
 * Tiny data-fetching hook (a stand-in for React Query / SWR).
 *   const { data, loading, error, reload } = useApi(() => leadsApi.list(filters), [filters]);
 * `fn` must return a Promise. Because every call goes through src/services/api.js,
 * pages never call fetch directly; all server calls go through services/api.js.
 */
export function useApi(fn, deps = []) {
  const [state, setState] = useState({ data: null, loading: true, error: null });
  const [tick, setTick] = useState(0);
  useEffect(() => {
    let alive = true;
    setState((s) => ({ ...s, loading: true }));
    fn()
      .then((data) => alive && setState({ data, loading: false, error: null }))
      .catch((error) => alive && setState({ data: null, loading: false, error }));
    return () => { alive = false; };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [...deps, tick]);
  const reload = useCallback(() => setTick((t) => t + 1), []);
  return { ...state, reload };
}
