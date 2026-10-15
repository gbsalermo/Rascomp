-- Regularização de participantes aprovados sem inscrição competitiva.
ALTER TABLE participant_competition_registrations
  ADD COLUMN competitive_link_suspended BOOLEAN NOT NULL DEFAULT FALSE,
  ADD COLUMN competitive_link_suspended_at DATETIME(6) NULL;
