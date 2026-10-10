After you edit Clojure source files, run `neorepl eval --port 7888 --timeout 180
"((requiring-resolve 'com.biffweb.tasks/agent-refresh))"` to evaluate the
changes and run linting/tests. (7888 is the app's nREPL port, `NREPL_PORT` in
`resources/config.edn`. Pass it explicitly: `.nrepl-port` can point at a
different server, e.g. one started by `neorepl server`.) If `neorepl` is not
already available, see https://metaredux.com/posts/2026/10/10/neorepl-a-terminal-repl-for-clojure-and-friends.html;
`trench` also works as a drop-in nREPL client. If the application is not yet
running, start it with `clojure -M:run dev` and keep it running until you're
done developing. When you've finished making changes, run
`clojure -M:run code-quality` and ensure it passes. If you started the app with
`clojure -M:run dev`, close it after you're done.
