import { useEffect, useState } from 'react';
import { api, STATUSES, PRIORITIES, allowedNext } from './api.js';

function ErrorBanner({ message, onClose }) {
  if (!message) return null;
  return (
    <div className="error">
      <span>{message}</span>
      <button onClick={onClose} aria-label="Dismiss error">×</button>
    </div>
  );
}

function TicketForm({ onCreated, onError }) {
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [priority, setPriority] = useState('MEDIUM');
  const [assignee, setAssignee] = useState('');

  async function submit(e) {
    e.preventDefault();
    try {
      const created = await api.create({
        title,
        description,
        priority,
        assignee: assignee || null
      });
      setTitle('');
      setDescription('');
      setAssignee('');
      onCreated(created);
    } catch (err) {
      onError(err.message);
    }
  }

  return (
    <form className="card" onSubmit={submit}>
      <h2>Create ticket</h2>
      <label>
        Title*
        <input value={title} onChange={(e) => setTitle(e.target.value)} maxLength={200} required />
      </label>
      <label>
        Description*
        <textarea
          value={description}
          onChange={(e) => setDescription(e.target.value)}
          maxLength={4000}
          required
        />
      </label>
      <div className="row">
        <label>
          Priority
          <select value={priority} onChange={(e) => setPriority(e.target.value)}>
            {PRIORITIES.map((p) => (
              <option key={p} value={p}>{p}</option>
            ))}
          </select>
        </label>
        <label>
          Assignee
          <input
            value={assignee}
            onChange={(e) => setAssignee(e.target.value)}
            placeholder="e.g. ada"
            maxLength={100}
          />
        </label>
      </div>
      <button type="submit">Create</button>
    </form>
  );
}

function Detail({ ticket, onChange, onError, onBack }) {
  const [edit, setEdit] = useState({
    title: ticket.title,
    description: ticket.description,
    priority: ticket.priority,
    assignee: ticket.assignee || ''
  });
  const [commentBody, setCommentBody] = useState('');
  const [commentAuthor, setCommentAuthor] = useState('');

  useEffect(() => {
    setEdit({
      title: ticket.title,
      description: ticket.description,
      priority: ticket.priority,
      assignee: ticket.assignee || ''
    });
  }, [ticket.id]); // eslint-disable-line react-hooks/exhaustive-deps

  async function saveEdit(e) {
    e.preventDefault();
    try {
      const updated = await api.update(ticket.id, {
        title: edit.title,
        description: edit.description,
        priority: edit.priority,
        assignee: edit.assignee
      });
      onChange(updated);
    } catch (err) {
      onError(err.message);
    }
  }

  async function transition(next) {
    try {
      const updated = await api.changeStatus(ticket.id, next);
      onChange(updated);
    } catch (err) {
      onError(err.message);
    }
  }

  async function addComment(e) {
    e.preventDefault();
    try {
      await api.addComment(ticket.id, {
        body: commentBody,
        author: commentAuthor || null
      });
      setCommentBody('');
      const refreshed = await api.get(ticket.id);
      onChange(refreshed);
    } catch (err) {
      onError(err.message);
    }
  }

  const next = allowedNext(ticket.status);

  return (
    <div className="card">
      <button className="link" onClick={onBack}>← Back to list</button>
      <h2>#{ticket.id} — {ticket.title}</h2>
      <p className="meta">
        Status: <strong>{ticket.status}</strong> · Priority: {ticket.priority} ·
        Assignee: {ticket.assignee || '—'}
      </p>

      <div className="row">
        {STATUSES.map((s) => (
          <button
            key={s}
            disabled={s === ticket.status}
            title={next.includes(s) ? 'Allowed transition' : 'Will be rejected by backend'}
            onClick={() => transition(s)}
          >
            → {s}
          </button>
        ))}
      </div>
      <p className="hint">Allowed next: {next.length ? next.join(', ') : 'none (terminal state)'}. Other buttons demonstrate backend rejection.</p>

      <form onSubmit={saveEdit}>
        <label>
          Title
          <input
            value={edit.title}
            onChange={(e) => setEdit({ ...edit, title: e.target.value })}
            maxLength={200}
          />
        </label>
        <label>
          Description
          <textarea
            value={edit.description}
            onChange={(e) => setEdit({ ...edit, description: e.target.value })}
            maxLength={4000}
          />
        </label>
        <div className="row">
          <label>
            Priority
            <select
              value={edit.priority}
              onChange={(e) => setEdit({ ...edit, priority: e.target.value })}
            >
              {PRIORITIES.map((p) => (
                <option key={p} value={p}>{p}</option>
              ))}
            </select>
          </label>
          <label>
            Assignee
            <input
              value={edit.assignee}
              onChange={(e) => setEdit({ ...edit, assignee: e.target.value })}
              maxLength={100}
            />
          </label>
        </div>
        <button type="submit">Save changes</button>
      </form>

      <h3>Comments ({ticket.comments?.length || 0})</h3>
      <ul className="comments">
        {(ticket.comments || []).map((c) => (
          <li key={c.id}>
            <strong>{c.author || 'anonymous'}</strong>{' '}
            <span className="meta">{new Date(c.createdAt).toLocaleString()}</span>
            <p>{c.body}</p>
          </li>
        ))}
      </ul>
      <form onSubmit={addComment}>
        <label>
          New comment*
          <textarea
            value={commentBody}
            onChange={(e) => setCommentBody(e.target.value)}
            maxLength={2000}
            required
          />
        </label>
        <label>
          Author
          <input
            value={commentAuthor}
            onChange={(e) => setCommentAuthor(e.target.value)}
            maxLength={100}
            placeholder="optional"
          />
        </label>
        <button type="submit">Add comment</button>
      </form>
    </div>
  );
}

export default function App() {
  const [tickets, setTickets] = useState([]);
  const [status, setStatus] = useState('');
  const [query, setQuery] = useState('');
  const [selected, setSelected] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  async function refresh(statusFilter = status, q = query) {
    setLoading(true);
    setError('');
    try {
      const data = await api.list({
        status: statusFilter || undefined,
        q: q || undefined
      });
      setTickets(data);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    refresh('', '');
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  function handleCreated(created) {
    setTickets((prev) => [created, ...prev]);
    setSelected(created);
  }

  function handleChanged(updated) {
    setSelected(updated);
    setTickets((prev) => prev.map((t) => (t.id === updated.id ? updated : t)));
  }

  return (
    <div className="container">
      <header>
        <h1>Support Ticket System</h1>
        <p className="meta">Backend: Spring Boot :8080 · Frontend: Vite :5173 · DB: H2 file</p>
      </header>

      <ErrorBanner message={error} onClose={() => setError('')} />

      {selected ? (
        <Detail
          ticket={selected}
          onChange={handleChanged}
          onError={setError}
          onBack={() => {
            setSelected(null);
            refresh();
          }}
        />
      ) : (
        <>
          <TicketForm onCreated={handleCreated} onError={setError} />
          <div className="card">
            <h2>Tickets</h2>
            <div className="row">
              <label>
                Search
                <input
                  value={query}
                  onChange={(e) => setQuery(e.target.value)}
                  placeholder="keyword in title/description"
                />
              </label>
              <label>
                Status filter
                <select value={status} onChange={(e) => setStatus(e.target.value)}>
                  <option value="">All</option>
                  {STATUSES.map((s) => (
                    <option key={s} value={s}>{s}</option>
                  ))}
                </select>
              </label>
              <button onClick={() => refresh()}>Apply</button>
              <button
                className="secondary"
                onClick={() => {
                  setStatus('');
                  setQuery('');
                  refresh('', '');
                }}
              >
                Reset
              </button>
            </div>
            {loading ? (
              <p>Loading…</p>
            ) : tickets.length === 0 ? (
              <p>No tickets yet. Create one above.</p>
            ) : (
              <table>
                <thead>
                  <tr>
                    <th>ID</th>
                    <th>Title</th>
                    <th>Status</th>
                    <th>Priority</th>
                    <th>Assignee</th>
                  </tr>
                </thead>
                <tbody>
                  {tickets.map((t) => (
                    <tr key={t.id} onClick={() => setSelected(t)} className="clickable">
                      <td>{t.id}</td>
                      <td>{t.title}</td>
                      <td><span className={`pill ${t.status}`}>{t.status}</span></td>
                      <td>{t.priority}</td>
                      <td>{t.assignee || '—'}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        </>
      )}
    </div>
  );
}
