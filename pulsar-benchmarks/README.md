# Pulsar Benchmarks

Micro benchmarks (JMH) for critical code paths.

## 1. Quick Run
```bash
# Build shaded benchmark jar (skip regular tests for speed)
./mvnw -pl pulsar-benchmarks -am package -DskipTests
# Run all benchmarks (1 fork, 3 warmups, 5 measurement iterations)
java -jar pulsar-benchmarks/target/pulsar-benchmarks-*-shaded.jar -f 1 -wi 3 -i 5
```
Optional JSON result output (for diff / storage):
```bash
SHA=$(git rev-parse --short HEAD)
TS=$(date +%Y%m%d-%H%M)
mkdir -p results
java -jar pulsar-benchmarks/target/pulsar-benchmarks-*-shaded.jar \
  -f 1 -wi 3 -i 5 -rf json -rff results/bench-${TS}-${SHA}.json
```

## 2. Conventions
- Keep benchmarks minimal and deterministic
- Avoid network / disk I/O inside benchmark methods
- Pre-compute test data in `@Setup` methods (`Level.Trial` unless per-iteration is intended)
- Name pattern: `<Domain><Operation>Benchmark` (e.g. `SelectorCompileBenchmark`)
- Always add a short Javadoc/KDoc: purpose + metric + potential regression trigger
- Use `@Param` for scale factors (e.g. input size / complexity)
- Avoid logging in benchmark loops
- Use `Blackhole` (or return values) to prevent dead code elimination (DCE)

## 3. Current State (Baseline)
| Aspect | Status |
|--------|--------|
| Module setup | ✅ `pulsar-benchmarks` added (excluded from deploy profile) |
| JMH integration | ✅ `jmh-core` + shaded executable jar |
| Example | ✅ `StringOpsBenchmark` (string concatenation & hash) |
| Output formats | ▶ Only stdout (JSON option documented) |
| Result storage | ❌ Not yet standardized (planned: `results/`) |
| Diff tooling | ❌ Not implemented |
| Threshold / budget | ❌ Not defined |
| Domain coverage | ▶ String + Selector skeleton + HTML skeleton (others pending) |
| CI integration | ❌ None (manual run) |

Legend: ✅ done | ▶ partial | ❌ pending

## 4. Principles & Anti‑Patterns
| Do | Avoid |
|----|------|
| Use `@State(Scope.Thread)` unless shared state needed | Reusing mutable shared objects w/o clear reason |
| Parametrize input size via `@Param` | Hard-coding only one size (no scalability insight) |
| Pre-generate inputs in `@Setup` | Allocating test data in benchmark body |
| Use `Blackhole.consume()` / return result | Relying on side-effects only |
| Small warmup first (e.g. 3) then measure | Excessive warmups hiding real startup cost |
| Keep benchmark logic < 30 LOC | Embedding large business flows (macro tests) |
| Separate micro vs scenario benchmarks | Mixing unrelated concerns in single class |

Common pitfalls:
1. Dead Code Elimination (DCE) – no return / blackhole
2. Constant Folding – precomputed constants vs dynamic creation
3. Allocation noise – generating data inside `@Benchmark`
4. JIT warmup insufficient – too few iterations
5. Measurement contamination – I/O, logging, randomness each invocation

## 5. Structure & Naming (Planned)
Proposed package layout (incremental migration):
```
ai.platon.pulsar.bench.selector   # selector & matching
ai.platon.pulsar.bench.dom        # DOM traversal / tree ops
ai.platon.pulsar.bench.html       # HTML parsing / tokenizing
ai.platon.pulsar.bench.ql         # query language parsing / planning
ai.platon.pulsar.bench.scoring    # scoring heuristics / rankings
ai.platon.pulsar.bench.extract    # content extraction / boilerplate removal
```

## 6. Template Support (Planned)
Introduce an abstract helper (Kotlin or Java) to reduce duplication:
- Reusable seeded random generator
- Utility `repeat(times) { }`
- Common `@Param` arrays: sizes (`16,128,1024`), complexity levels (`SMALL,MEDIUM,LARGE`)
- Optional data factory hooks (`@Setup`) for large payload reuse

## 7. Output & Storage
- Directory: `pulsar-benchmarks/results/` (ignored by VCS except summary markdown if needed)
- Raw files: `bench-<yyyyMMdd-HHmm>-<gitSha>.json`
- Future artifact: aggregated diff report `bench-report.md`

## 8. Regression Detection (Planned Steps)
1. Standardize JSON output (already documented)
2. Add diff script: compares last two JSON files → prints Δ% sorted by magnitude
3. Introduce budget file `benchmarks-budget.yml`:
   ```yaml
   StringOpsBenchmark.builderConcat: {minThroughput: 100000.0, maxRegressionPct: 10}
   ```
4. Soft alarm in CI (warn only)
5. Hard gate optional (block if critical path regression > threshold)

## 9. Roadmap (Phased)
| Phase | ID | Item | Value | Difficulty |
|-------|----|------|-------|------------|
| P0 | 1 | Package structure & naming pass | High | Low |
| P0 | 2 | Benchmark template base class | High | Low |
| P0 | 3 | Principles doc (this section) | High | Low |
| P0 | 4 | JSON storage + diff script | High | Medium |
| P0 | 5 | Selector & HTML parse baseline benchmarks | High | Medium |
| P1 | 6 | Budget file + soft CI alarm | High | Medium |
| P1 | 7 | GC profiling guidelines (`-prof gc`) | Medium | Low |
| P1 | 8 | QL parse & scoring benchmarks | High | Medium |
| P2 | 9 | History archive + trend table | High | Medium |
| P2 | 10 | Cross JDK / GC matrix script | Medium | Medium |
| P2 | 11 | Flame graph / async-profiler integration | High | Med-High |
| P3 | 12 | Selective benchmark run by git diff | High | High |

## 10. Candidate Benchmarks
| Domain | Benchmark | Focus | Notes |
|--------|-----------|-------|-------|
| Selector | `SelectorCompileBenchmark` | Parsing / compilation | Complex selector sets |
| Selector | `SelectorMatchBenchmark` | Matching speed | Param: node fan-out |
| HTML | `HtmlParseBenchmark` | Tokenization & tree build | Param: document size |
| DOM | `DomTraverseBenchmark` | Traversal throughput | BFS / DFS variants |
| QL | `QlParseBenchmark` | Syntax to AST | Param: query length |
| Scoring | `ScoreEvalBenchmark` | Heuristic scoring loop | Param: candidates count |
| Extract | `ContentExtractBenchmark` | Boilerplate removal | Param: HTML noise ratio |
| Strings | `StringOpsBenchmark` | Allocation & concat strategy | Existing |

## 11. Contribution Checklist
Before submitting a new benchmark:
- [ ] Name follows `<Domain><Operation>Benchmark`
- [ ] Contains Javadoc: purpose + metric + regression triggers
- [ ] Uses `@Param` for at least one scale dimension
- [ ] No I/O / logging inside `@Benchmark`
- [ ] Test data prepared in `@Setup`
- [ ] Returns value or uses Blackhole
- [ ] Runtime < ~5s per benchmark class (default config)
- [ ] Added to README candidate table (if new domain)

## 12. CI Integration (Future)
Suggested GitHub Actions optional workflow:
1. Build benchmarks (skip tests)
2. Run targeted subset (changed domains)
3. Store JSON artifact + diff vs main baseline
4. Comment on PR with regression summary
5. (Optional) Fail if `critical=true` & regression > budget

## 13. JMH Flag Cheat Sheet
| Flag | Meaning | Typical Value |
|------|---------|---------------|
| `-f` | Forks | `1` (more for stability) |
| `-wi` | Warmup iterations | `3`–`5` |
| `-w` | Warmup time | `1s`–`2s` |
| `-i` | Measurement iterations | `5`–`10` |
| `-r` | Measurement time | `1s`–`3s` |
| `-t` | Threads | `1` or CPU core count |
| `-rf json` | Result format | JSON export |
| `-rff file` | Result file | Path to JSON/CSV |
| `-prof gc` | GC profiler | Allocation & GC stats |

## 14. Example
See `ai.platon.pulsar.bench.StringOpsBenchmark`.

Upcoming examples will illustrate selector compilation & HTML parsing once underlying utility hooks are extracted.

## 15. FAQ (Seed Items)
| Question | Answer |
|----------|--------|
| Why only 1 fork? | Faster feedback; increase to 2–3 for noisy benches. |
| Why not macro/E2E here? | This module focuses on micro/meso; macro belongs in integration/perf suites. |
| Should I profile every run? | No—use profiling selectively when regression suspected. |

## 16. Next Immediate Actions (Recommended to Tackle in Order)
Completed P0 (initial): 1) package scaffolds 2) base helper 3) selector & html skeletons ✅
Remaining immediate tasks:
4. Add diff script (store last two runs)
5. Draft `benchmarks-budget.yml` (empty thresholds first)
6. Add GC profiling guideline snippet in README (flags & sample command)
7. Implement additional domain skeletons: `QlParseBenchmark`, `DomTraverseBenchmark`

---
_This document will evolve as benchmarks mature. Keep changes incremental; avoid large unsolicited refactors._
