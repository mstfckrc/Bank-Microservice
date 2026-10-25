import "server-only";

import { ClientSecretPost, discovery, type Configuration } from "openid-client";

let configurationPromise: Promise<Configuration> | undefined;

function requiredEnvironment(name: "OAUTH_ISSUER_URI" | "KEYCLOAK_CLIENT_ID" | "KEYCLOAK_CLIENT_SECRET"): string {
  const value = process.env[name];
  if (!value) throw new Error("OIDC configuration is incomplete");
  return value;
}

export function getOidcConfiguration(): Promise<Configuration> {
  if (!configurationPromise) {
    const issuer = new URL(requiredEnvironment("OAUTH_ISSUER_URI"));
    if (issuer.protocol !== "https:" || issuer.username || issuer.password || issuer.search || issuer.hash) {
      throw new Error("OIDC issuer must be a clean HTTPS URL");
    }

    const clientId = requiredEnvironment("KEYCLOAK_CLIENT_ID");
    const clientSecret = requiredEnvironment("KEYCLOAK_CLIENT_SECRET");
    configurationPromise = discovery(issuer, clientId, undefined, ClientSecretPost(clientSecret))
      .catch((error: unknown) => {
        configurationPromise = undefined;
        throw error;
      });
  }
  return configurationPromise;
}
