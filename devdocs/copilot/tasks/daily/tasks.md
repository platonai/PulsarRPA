# Tasks

## 0. Prerequisites

Read root README-AI.md and `devdocs/copilot/test-guide.md` for guidelines

## docs & comments

- generate detailed comments for DomService and ChromeCdpDomService

## feature

### refine `ClickableElementDetector`

- `ClickableElementDetectorTest` for basic tests
- `ClickableElementDetectorE2ETest` for e2e
  - use real page `interactive-dynamic.html`
  - read `interactive-dynamic.html` to design the tests
  - write tests with the same pattern with `ChromeDomServiceIsScrollableTest`
