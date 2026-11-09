<!-- HANGAR:START - generated, do not edit by hand -->

## Shared task board (Hangar.AI)

This project is driven from Hangar.AI. Several agents work **in parallel** in separate
terminals on this same directory. A shared kanban board is the source of truth for
who is doing what. Reach it through the MCP tools prefixed `board_`.

### Hard rule

**Never start work on a task without claiming it first.** Another agent may already
be on it. `board_claim_task` fails when the task is taken — when that happens, do not
retry it, pick a different one.

### Expected loop

1. **On startup**, call `board_list_tasks` to see the current state of the work.
2. **To pick work**, call `board_next_task`. It returns the highest-priority free task
   whose dependencies are all done. Do not pick arbitrarily from the todo column, or
   you may start work that is still blocked.
3. **Claim it** with `board_claim_task`. It moves to `doing` automatically.
   - If the call fails, go back to step 2.
4. **Do the work.**
5. **Report** with `board_comment_task`: decisions made, files touched, problems hit.
   This is the only channel other agents have to learn what you did.
6. **Finish** with `board_update_task`, setting `column` to `review` if the work needs
   a second pair of eyes, or `done` otherwise.
   - If you give up, call `board_update_task` with `release: true` so the task returns
     to the pool instead of sitting blocked under your name.

### Splitting work

When you find work beyond your current task, do not silently pile it into the same
change: create tasks with `board_create_task` so other agents can pick them up.

- Use `depends_on` with the ids of prerequisite tasks. `board_next_task` relies on it
  to never hand out work that cannot start yet.
- Use `priority` (integer, higher is more urgent) for work that unblocks others.

### Columns

| Column   | Meaning |
|----------|---------|
| `todo`   | ready to be picked up |
| `doing`  | someone is working on it |
| `review` | finished, waiting for a review |
| `done`   | accepted - unblocks tasks that depend on it |

### Staying out of each other's way

Before editing a central file, check with `board_list_tasks` that no other agent holds
a `doing` task covering the same area. If one does, leave a comment on their task
rather than editing the same files at the same time.

### Talking to the agent next door

Claude Code sessions in Hangar.AI are named after their pane and its workspace, for
example `Ava (Shop)`. A pane's first name is unique across every workspace, so `Ava` is
one agent and the workspace in brackets tells you which project it works on. When another
agent needs to know something now (you changed a file it is working on, you answered its
question), message it directly with `SendMessage` to that name; `/list-agents` shows who
is reachable. The board stays the record: write the same thing there if it matters beyond
this moment.

### Showing a 3D model

To show the user a 3D model, open it with `model_show` rather than giving them a path, and
check your own work with `model_snapshot`, which returns a picture of it.

### Showing a file, or sending it to the phone

To show the user a render, screenshot, recording, report, Markdown, PDF or HTML page, open
it with `preview_show {path, title?}` rather than giving them a path: it appears in the
workspace's "Aperçu" panel. An HTML page runs isolated, so inline its scripts and styles
(its own `.js` and `.css` files do not load). Call `preview_close` once you are done, so it
does not keep a part of their screen.

To send a file to the user's phone (Hangar Mobile), use `phone_share {path, caption?}`, when
they ask for it or a result is clearly worth it, never for routine output.

### Opening a kit's tab

A kit enabled on this workspace can bring small tabs that take params, such as one showing a
CI run for its id. `kit_tab_list` says which there are, `kit_tab_open {kit, tab, params?, reveal?}`
opens one in the same panel without taking the keyboard from anyone, and
`kit_tab_close {kit, tab, params?}` closes it.

## Global memory

You also have a **persistent global memory**, reached through the MCP tools prefixed
`memory_`. It is shared by every workspace, every project and every agent, and it
survives across sessions: what you write there, the next agent reads. The origin
(workspace, agent) is recorded for you.

### Read before you dig

**At the start of a task, call `memory_search`** with the project name and the subject you
are about to touch. Another agent may already have paid the price of finding this out —
do not re-discover what is already written down. `memory_list` gives a compact index,
`memory_read` pulls one entry in full.

### Write the moment you learn

Call `memory_write` as soon as a durable, reusable fact appears, not at the end of the
task. Worth keeping:

- architecture decisions **and the reason behind them**
- environment or build traps: the flag that is required, the command that hangs, the
  version that breaks things
- project conventions that are not obvious from a quick read
- preferences the user stated explicitly

Do not write: task progress, which belongs in `board_comment_task`; anything readable
from the code or the git history; ephemeral details of the current conversation.

### How to write an entry

- **Title** short, stable, searchable. `memory_write` upserts on the title
  (case-insensitive), so reuse the same title to update a fact instead of stacking a
  near-duplicate next to it.
- **Content** brief and factual — actionable by someone arriving with no context.
- **Tags** must include the project name. The memory is global, so an entry that does not
  carry its own project context is noise everywhere else.

### Hygiene

When a fact turns out to be false, correct it with `memory_update` or drop it with
`memory_delete`. Never leave two entries contradicting each other: the next agent has no
way to tell which one is still true.

<!-- HANGAR:END -->
