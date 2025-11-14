# Pull Request Description Template

```
## Summary
<Concise summary of the change>

## Motivation
<Why this change is needed / problem solved>

## Change Details
- <Bullet 1>
- <Bullet 2>

## Type of Change
- [ ] Feature
- [ ] Bug Fix
- [ ] Refactor
- [ ] Performance
- [ ] Test / CI
- [ ] Docs
- [ ] Security
- [ ] Other: <specify>

## Breaking Change
- [ ] NO
- [ ] YES (Describe impact and migration path):

## Screenshots / Logs (Optional)
<Attach if UI / behavior oriented>

## Test Coverage
- Added Tests: <List>
- Modified Tests: <List>
- Tags Executed: <IntegrationTest / E2ETest>

## Verification
Commands executed:
```
./mvnw -q verify
```
Result: <PASS/FAIL>

## Performance Considerations
<None | Impact description + benchmark refs>

## Security Considerations
<None | Input validation added | Sanitization | etc>

## Risks & Mitigations
- Risk: <Description> | Mitigation: <Strategy>

## Rollback Plan
`git revert <commit>` (No schema / data migration)
OR
<Describe manual rollback>

## Follow-ups (Optional)
- <Next improvement>
- <Deferred item>
```

