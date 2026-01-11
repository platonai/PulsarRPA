# Browser Agent Use Case Compendium

This document provides a comprehensive collection of browser agent use cases,
organized by complexity and deployment maturity.

The cases are grouped into:
- Simple (single-site, deterministic)
- Complex (agentic reasoning, loops, aggregation)
- Enterprise (cross-site, long-running, auditable)

All examples are expressed as executable step-based workflows.

---

## Level 1 — Simple Use Cases

### 1. E-commerce Product Comparison (Single-site)
1. go to https://www.amazon.com/
2. search for “mechanical keyboard”
3. open the first 3 products
4. extract price, rating, and review count
5. write a comparison table to a markdown file

---

### 2. Job Listing Extraction (Single-site)
1. go to https://www.linkedin.com/jobs
2. search for “Kotlin backend engineer”
3. open the first 5 job postings
4. extract company, location, and skills
5. export the result as JSON

---

### 3. SaaS Pricing Analysis (Single-site)
1. go to https://www.notion.so/pricing
2. identify all pricing tiers
3. extract price and core features
4. summarize differences in markdown

---

### 4. Community Sentiment Scan (Single-site)
1. go to https://www.reddit.com/r/programming/
2. search for “browser automation”
3. open the top 5 posts
4. summarize key opinions and sentiment

---

### 5. Cloud Console Alert Check (Single-site)
1. go to the cloud provider console
2. log in with credentials
3. navigate to the alerts page
4. export active alerts to CSV

---

## Level 2 — Complex Use Cases (Single-site)

### 6. Deep E-commerce Category Analysis
1. go to https://www.amazon.com/
2. search for “noise cancelling headphones”
3. filter by rating ≥ 4 stars
4. sort by “Best Sellers”
5. open the top 5 products
6. extract pricing and review summaries
7. identify common negative keywords
8. rank products by value
9. generate a markdown report

---

### 7. Job Market Skill Demand Analysis
1. go to https://www.linkedin.com/jobs
2. search for “Senior Backend Engineer”
3. apply “Remote” filter
4. paginate through 3 pages
5. open 10 job postings
6. extract skill requirements
7. normalize skill names
8. calculate frequency
9. generate a demand summary

---

### 8. Open Source Project Health Evaluation
1. go to https://github.com/search
2. search for “browser automation”
3. filter by stars > 1000
4. open the top 3 repositories
5. navigate to Issues
6. count open vs closed issues
7. navigate to Contributors
8. evaluate activity level
9. produce a health score

---

## Level 2 — Complex Use Cases (Cross-site)

### 9. Competitive Product Analysis
1. go to https://www.producthunt.com/
2. search for “AI agent”
3. select the top 3 products
4. extract tagline and category
5. visit each official website
6. locate pricing pages
7. extract pricing and positioning
8. compare feature focus
9. generate a comparison matrix

---

### 10. Technology Trend Validation
1. search on Google for “headless browser framework”
2. collect top mentioned projects
3. go to https://www.github.com/
4. open each project repository
5. extract stars and forks
6. inspect recent commits
7. assess community activity
8. identify growth or decline
9. write a trend analysis summary

---

### 11. Hiring Market Cross-Platform Comparison
1. go to https://www.indeed.com/
2. search for “QA automation engineer”
3. record salary ranges
4. go to https://www.linkedin.com/jobs
5. search for the same role
6. compare skill requirements
7. assess demand differences
8. summarize findings

---

## Level 3 — Enterprise Use Cases

### 12. Company Due Diligence Automation
1. go to the company official website
2. extract product description and customers
3. go to https://www.crunchbase.com/
4. extract funding history and investors
5. search the company on Google News
6. open the latest 5 articles
7. identify major business events
8. assess risk and maturity
9. generate a due diligence report

---

### 13. Ongoing Competitive Monitoring
1. go to competitor official websites
2. locate pricing and feature pages
3. snapshot current state
4. repeat on a scheduled interval
5. detect changes and diffs
6. classify changes (pricing / feature)
7. store versioned results
8. trigger alerts
9. generate a periodic report

---

### 14. Enterprise Operations Inspection
1. go to internal admin or ops dashboard
2. authenticate with enterprise SSO
3. navigate across multiple subsystems
4. collect system status and metrics
5. identify anomalies
6. retry failed checks
7. log all actions
8. export audit records
9. notify stakeholders

---

## Capability Coverage Matrix

| Capability | Simple | Complex | Enterprise |
|---|---|---|---|
| Page navigation | ✓ | ✓ | ✓ |
| User interaction | ✓ | ✓ | ✓ |
| Pagination / loops | – | ✓ | ✓ |
| Conditional branching | – | ✓ | ✓ |
| Cross-site workflows | – | ✓ | ✓ |
| Long-running agents | – | – | ✓ |
| Observability | – | – | ✓ |
| Audit & replay | – | – | ✓ |

---

## Summary

Browser agents evolve from deterministic scripts,
to reasoning-driven research tools,
and finally to enterprise-grade, observable systems.

This progression enables teams to:
- start small,
- scale intelligence,
- and operationalize browser automation as infrastructure.
