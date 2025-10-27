package app.routes.handling;

import app.security.enums.Role;
import io.javalin.apibuilder.ApiBuilder;
import io.javalin.apibuilder.EndpointGroup;
import io.javalin.http.Handler;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/*
Custom api/routes overview since the default javalin one does not show roles anymore after AccessManager was removed.
It just displayed empty Array. Now this custom overview has that feature and cool inline styling.
 */
public final class RouteDocs {
    private RouteDocs() {}

    // --- route registry for /api/routes ---
    private record RouteEntry(String method, String path, List<String> roles) {}
    private static final List<RouteEntry> ROUTES = new CopyOnWriteArrayList<>();
    private static final Deque<String> PREFIX = new ArrayDeque<>();

    private static String fullPath(String leaf) {
        String base = PREFIX.stream()
                .map(s -> s.startsWith("/") ? s : "/" + s)
                .collect(Collectors.joining());
        String l = leaf.startsWith("/") ? leaf : "/" + leaf;
        return (base + l).replaceAll("//+", "/");
    }

    private static void addRoute(String method, String path, Role... roles) {
        List<String> names = (roles == null)
                ? List.of()
                : Arrays.stream(roles).map(Enum::name).toList();
        ROUTES.add(new RouteEntry(method, fullPath(path), names));
    }

    // --- role enforcement via RoleGuard ---
    private static Handler guarded(Handler h, Role... roles) {
        if (roles == null || roles.length == 0) return h;      // no restriction
        for (Role r : roles) if (r == Role.ANYONE) return h;   // public
        return ctx -> { RoleGuard.require(ctx, roles); h.handle(ctx); };
    }

    // --- DSL wrappers (use these via: import static app.routes.handling.RouteDocs.*;) ---
    public static void path(String path, EndpointGroup group) {
        String normalized = path.startsWith("/") ? path : "/" + path;
        PREFIX.push(normalized);
        ApiBuilder.path(path, group);
        PREFIX.pop();
    }

    public static void get(String path, Handler h, Role... roles) {
        addRoute("GET", path, roles);
        ApiBuilder.get(path, guarded(h, roles));
    }

    public static void post(String path, Handler h, Role... roles) {
        addRoute("POST", path, roles);
        ApiBuilder.post(path, guarded(h, roles));
    }

    public static void put(String path, Handler h, Role... roles) {
        addRoute("PUT", path, roles);
        ApiBuilder.put(path, guarded(h, roles));
    }

    public static void delete(String path, Handler h, Role... roles) {
        addRoute("DELETE", path, roles);
        ApiBuilder.delete(path, guarded(h, roles));
    }

    // ---- Pretty Overview Page (/api/routes) ----
    public static Handler overviewHtml = ctx -> {
        final String ctxBase = Optional.ofNullable(ctx.contextPath()).orElse(""); // "/api"


        // Sort by path, then by method
        List<RouteEntry> routes = ROUTES.stream()
                .sorted(Comparator.comparing(RouteEntry::path).thenComparing(RouteEntry::method))
                .toList();

        // Build the page without String.format/.formatted to avoid % issues
        String head = """
<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<title>API Routes</title>
<meta name="viewport" content="width=device-width, initial-scale=1">
<style>
  :root {
    --bg: #0f1115; --card: #141821; --text: #e6e6e6; --muted: #a0a4ab; --border: #222838;
    --meth-get:#3b82f6; --meth-post:#10b981; --meth-put:#f59e0b; --meth-del:#ef4444; --meth-patch:#8b5cf6;
    --role-admin:#ef4444; --role-guest:#06b6d4; --role-anyone:#9ca3af;
    --accent:#6ee7b7;
  }
  td a {
                    color: inherit;
                    text-decoration: none;
                  }
                  td a:visited {
                    color: inherit;
                  }
                  td a:hover,
                  td a:focus {
                    text-decoration: none;
                    filter: brightness(1.05); /* subtle hint it’s clickable */
                  }
  *{box-sizing:border-box}
  body{margin:0;background:var(--bg);color:var(--text);font:14px/1.45 system-ui, -apple-system, Segoe UI, Roboto, Ubuntu, "Helvetica Neue", Arial}
  .container{max-width:1100px;margin:32px auto;padding:0 16px}
  header{display:flex;align-items:center;justify-content:space-between;margin-bottom:16px}
  h1{font-size:22px;margin:0}
  .hint{color:var(--muted);font-size:13px}
  .card{background:var(--card);border:1px solid var(--border);border-radius:12px;overflow:hidden}
  .toolbar{display:flex;gap:8px;align-items:center;padding:12px 12px;border-bottom:1px solid var(--border)}
  .search{flex:1;display:flex;align-items:center;background:#0c0f14;border:1px solid var(--border);border-radius:8px;padding:6px 10px;color:var(--text)}
  .search input{flex:1;background:transparent;border:0;outline:none;color:inherit}
  table{width:100%;border-collapse:collapse}
  th, td{padding:10px 12px;border-bottom:1px solid var(--border);vertical-align:middle}
  th{color:var(--muted);text-align:left;font-weight:600;font-size:12px;letter-spacing:.04em;text-transform:uppercase}
  tr:hover td{background:rgba(255,255,255,.02)}
  code{background:#0d1117;border:1px solid var(--border);padding:2px 6px;border-radius:6px}
  .method{display:inline-block;font-weight:700;letter-spacing:.04em;font-size:11px;padding:4px 8px;border-radius:999px;color:#0b0f15}
  .GET{background:var(--meth-get)}
  .POST{background:var(--meth-post)}
  .PUT{background:var(--meth-put)}
  .DELETE{background:var(--meth-del)}
  .PATCH{background:var(--meth-patch)}
  .roles{display:flex;gap:6px;flex-wrap:wrap}
  .role{display:inline-block;border:1px solid var(--border);padding:4px 8px;border-radius:999px;font-size:12px;color:var(--muted)}
  .role.ADMIN{border-color:var(--role-admin);color:var(--role-admin)}
  .role.GUEST{border-color:var(--role-guest);color:var(--role-guest)}
  .role.ANYONE{border-color:var(--role-anyone);color:var(--role-anyone)}
  .empty{color:var(--muted)}
  footer{margin-top:10px;color:var(--muted);font-size:12px}
  .count{color:var(--accent);font-weight:600}
  .ctx{color:var(--muted);font-size:12px;padding-left:6px}
</style>
</head>
<body>
  <div class="container">
    <header>
      <h1>API Routes <span class="ctx">(context path: __CTX__)</span></h1>
      <div class="hint">__COUNT__ routes</div>
    </header>

    <div class="card">
      <div class="toolbar">
        <div class="search">
          <input id="q" placeholder="Filter by path, method or role…" autofocus>
        </div>
      </div>
      <table id="routes">
        <thead>
          <tr>
            <th style="width:110px;">Method</th>
            <th>Path</th>
            <th style="width:260px;">Roles</th>
          </tr>
        </thead>
        <tbody>
""";

        String tail = """
        </tbody>
      </table>
    </div>
    <footer>Showing <span class="count" id="count">__COUNT__</span> routes</footer>
  </div>

                <script>
                           // Elements
                           const q = document.getElementById('q');
                           const table = document.getElementById('routes');
                           const theadCells = table.querySelectorAll('thead th');
                           const tbody = table.querySelector('tbody');
                           let rows = Array.from(tbody.querySelectorAll('tr'));
                           const count = document.getElementById('count');
                
                           // --- Filtering (unchanged) ---
                           const filter = () => {
                             const term = (q.value || '').trim().toLowerCase();
                             let visible = 0;
                             for (const tr of rows) {
                               const hay = (tr.dataset.path + ' ' + tr.dataset.method + ' ' + tr.dataset.roles).toLowerCase();
                               const show = !term || hay.includes(term);
                               tr.style.display = show ? '' : 'none';
                               if (show) visible++;
                             }
                             count.textContent = visible;
                           };
                           q.addEventListener('input', filter);
                
                           // --- Sorting ---
                           // extractors for the three columns
                           const extract = {
                             method: tr => (tr.dataset.method || '').toUpperCase(),
                             path:   tr => (tr.dataset.path   || ''),
                             roles:  tr => ((tr.dataset.roles || '')
                                           .split(',').map(s => s.trim()).filter(Boolean).sort().join(',')) // normalize role order
                           };
                
                           // sort state: 1 = ascending, -1 = descending
                           const state = { method: 1, path: 1, roles: 1 };
                
                           function sortBy(key) {
                             rows.sort((a, b) => {
                               const A = extract[key](a);
                               const B = extract[key](b);
                               if (A < B) return -1 * state[key];
                               if (A > B) return  1 * state[key];
                               return 0;
                             });
                             state[key] *= -1;          // toggle direction for next click
                             rows.forEach(r => tbody.appendChild(r)); // reattach in new order
                             filter();                  // keep current filter active
                           }
                
                           // Make headers clickable
                           theadCells.forEach(th => th.style.cursor = 'pointer');
                           // 0 = Method, 1 = Path, 2 = Roles (your table order)
                           theadCells[0].addEventListener('click', () => sortBy('method'));
                           theadCells[1].addEventListener('click', () => sortBy('path'));
                           theadCells[2].addEventListener('click', () => sortBy('roles'));
                         </script>
</body>
</html>
""";

        StringBuilder html = new StringBuilder(8192);
        html.append(head
                .replace("__CTX__", esc(ctxBase))
                .replace("__COUNT__", String.valueOf(routes.size())));

        for (var r : routes) {
            String fullPathShown = ctxBase + r.path();
            String roleBadges = r.roles().isEmpty()
                    ? "<span class='empty'>—</span>"
                    : r.roles().stream()
                    .map(role -> "<span class='role " + esc(role) + "'>" + esc(role) + "</span>")
                    .collect(Collectors.joining(" "));

            html.append("""
        <tr data-path="%s" data-method="%s" data-roles="%s">
          <td><span class="method %s">%s</span></td>
          <td><a href="%s" target="_blank" rel="noopener"><code>%s</code></a></td>
          <td class="roles">%s</td>
        </tr>
        """.formatted(
                    esc(r.path()),
                    esc(r.method()),
                    esc(String.join(",", r.roles())),
                    esc(r.method()), esc(r.method()),
                    esc(fullPathShown),           // link target
                    esc(fullPathShown),           // visible text
                    roleBadges
            ));
        }

        html.append(tail.replace("__COUNT__", String.valueOf(routes.size())));

        ctx.contentType("text/html; charset=utf-8").result(html.toString());
    };

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}