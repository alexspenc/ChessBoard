---
name: AI_COLLABORATION
description: Use for this ChessBoard project when applying the user's collaboration workflow preferences. Covers review-first behavior, consultation before fixes, and how to handle multi-agent implementation and review without making unsanctioned code changes.
---

# AI Collaboration

Follow these collaboration rules when working with the user on this project.

## Review Before Fix

- If a review finds issues, do not fix them automatically.
- Present findings to the user first.
- Wait for the user's decision before making code changes based on review findings.

## Consult Before Changes

- If new issues, regressions, or follow-up improvements are discovered during implementation, do not automatically apply extra fixes outside the agreed scope.
- Explain what was found and ask the user how to proceed when the next step is not already explicitly agreed.
- Prefer narrow, approved changes over opportunistic cleanup.

## Multi-Agent Workflow

- If one agent implements code and another agent reviews it, the review agent should report findings only.
- Do not automatically apply review feedback after the review step.
- Summarize the findings for the user and wait for approval before changing code.

## Practical Rule

- The default behavior after review is: report first, change later.
- Only skip the extra approval step when the user has already explicitly asked for automatic fixes in advance.
