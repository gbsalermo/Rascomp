import { DurableObject, scheduler } from "cloudflare:workers";

const INSTANCE_NAME = "primary";
const PORT = 8080;
const INACTIVITY_TIMEOUT_MS = 30 * 60 * 1000;

function required(env, name) {
  const value = env[name];
  if (typeof value !== "string" || value.trim() === "") {
    throw new Error(`Cloud configuration '${name}' is required before starting RasComp.`);
  }
  return value;
}

function optional(env, name, fallback = "") {
  const value = env[name];
  return typeof value === "string" ? value : fallback;
}

export class RascompApiContainer extends DurableObject {
  starting;

  constructor(ctx, env) {
    super(ctx, env);
    const container = ctx.container;
    if (container?.running) {
      void ctx.blockConcurrencyWhile(() =>
        container.setInactivityTimeout(INACTIVITY_TIMEOUT_MS),
      );
    }
  }

  async fetch(request) {
    this.starting ??= this.startAndWaitForPort().finally(() => {
      this.starting = undefined;
    });

    await this.starting;

    const url = new URL(request.url);
    url.protocol = "http:";
    url.host = "container";

    const forwarded = new Request(url, request);
    forwarded.headers.delete("host");

    return this.ctx.container.getTcpPort(PORT).fetch(forwarded);
  }

  async startAndWaitForPort() {
    const container = this.ctx.container;

    if (!container.running) {
      container.start({
        image: container.images.base,
        instance: "lite",
        enableInternet: true,
        env: {
          SPRING_PROFILES_ACTIVE: "cloud",
          PORT: String(PORT),

          DB_URL: required(this.env, "DB_URL"),
          DB_USERNAME: required(this.env, "DB_USERNAME"),
          DB_PASSWORD: required(this.env, "DB_PASSWORD"),
          JWT_SECRET: required(this.env, "JWT_SECRET"),

          CORS_ALLOWED_ORIGINS: required(this.env, "CORS_ALLOWED_ORIGINS"),
          IDENTITY_FRONTEND_BASE_URL: required(this.env, "IDENTITY_FRONTEND_BASE_URL"),

          RASCOMP_DEV_NOME: optional(this.env, "RASCOMP_DEV_NOME", "Administrador RasComp"),
          RASCOMP_DEV_EMAIL: required(this.env, "RASCOMP_DEV_EMAIL"),
          RASCOMP_DEV_PASSWORD: required(this.env, "RASCOMP_DEV_PASSWORD"),

          EMAIL_PROVIDER: optional(this.env, "EMAIL_PROVIDER", "log"),
          EMAIL_FROM: optional(this.env, "EMAIL_FROM", "RasComp <no-reply@localhost>"),
          RESEND_API_KEY: optional(this.env, "RESEND_API_KEY"),

          R2_ENABLED: optional(this.env, "R2_ENABLED", "false"),
          R2_ACCOUNT_ID: optional(this.env, "R2_ACCOUNT_ID"),
          R2_ENDPOINT: optional(this.env, "R2_ENDPOINT"),
          R2_ACCESS_KEY_ID: optional(this.env, "R2_ACCESS_KEY_ID"),
          R2_SECRET_ACCESS_KEY: optional(this.env, "R2_SECRET_ACCESS_KEY"),
          R2_BUCKET: optional(this.env, "R2_BUCKET"),
          R2_PUBLIC_BASE_URL: optional(this.env, "R2_PUBLIC_BASE_URL"),

          SWAGGER_ENABLED: optional(this.env, "SWAGGER_ENABLED", "false"),
        },
      });
    }

    await container.setInactivityTimeout(INACTIVITY_TIMEOUT_MS);

    const port = container.getTcpPort(PORT);
    let lastError;

    for (let attempt = 0; attempt < 120; attempt++) {
      try {
        const response = await port.fetch("http://container/actuator/health", {
          signal: AbortSignal.timeout(1000),
        });
        await response.body?.cancel();

        if (response.ok) return;
        throw new Error(`Health check returned ${response.status}`);
      } catch (error) {
        lastError = error;
        await scheduler.wait(250);
      }
    }

    throw new Error("RasComp container did not become ready.", {
      cause: lastError,
    });
  }
}

export default {
  fetch(request, env) {
    return env.RASCOMP_API.getByName(INSTANCE_NAME).fetch(request);
  },
};
