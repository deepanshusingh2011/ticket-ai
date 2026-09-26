# /review-spec — TicketAI Spec Review

Checklist + prompt for reviewing doc changes. Frozen file: `SPEC.md` (never edit).

## Checklist

- [ ] `SPEC.md` untouched (`git status` / `git diff --name-only` shows no modification).
- [ ] Change made in `docs/SPEC_CONSOLIDATED.md` first, mirrored to matching `spec/*.md`
      (contract→`api-contract.md`, transitions→`state-machine.md`, fields→`data-model.md`+`requirements.md`,
      tests→`test-strategy.md`).
- [ ] Transition table, endpoint table (§6.1), envelope keys, error shapes (§6.3), curls (§6.5),
      CORS origins, H2 URLs, 9 test names — identical across consolidated spec, `spec/*`, backend code.
- [ ] Acceptance criteria (§12) + verification (§14) updated if behavior changed.
- [ ] `docs/prompt-history.md` appended (not rewritten); direction consolidated → fragments respected.

## Prompt

```text
Review this TicketAI doc diff for spec consistency. Inputs: docs/SPEC_CONSOLIDATED.md, spec/*,
docs/prompt-history.md, git diff --name-only. Verify the checklist above; quote any
contradiction (file:line vs file:line) between the consolidated spec, spec/* fragments, and
backend code (TicketStatus/TicketService/TicketController/GlobalExceptionHandler). Confirm
SPEC.md is untouched and prompt-history was appended, not rewritten. Report PASS/FAIL per item.
```
