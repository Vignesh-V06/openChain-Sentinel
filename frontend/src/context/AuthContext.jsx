
import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
} from "react";

import {
  registerUser,
  loginUser,
  getCurrentUser,
  saveAuthToken,
  getAuthToken,
  clearAuthToken,
} from "../services/api";

const AuthContext = createContext(null);

function getPayload(response) {
  return response?.data ?? response;
}

function getToken(response) {
  const payload = getPayload(response);

  return (
    payload?.token ??
    payload?.accessToken ??
    payload?.jwt ??
    payload?.authToken ??
    payload?.data?.token ??
    payload?.data?.accessToken ??
    payload?.data?.jwt ??
    payload?.data?.authToken ??
    null
  );
}

function getUser(response) {
  const payload = getPayload(response);

  return (
    payload?.user ??
    payload?.data?.user ??
    payload?.data ??
    payload
  );
}

function getErrorMessage(error, fallback) {
  const data = error?.response?.data;

  if (typeof data === "string" && data.trim()) {
    return data;
  }

  return (
    data?.message ??
    data?.error ??
    data?.detail ??
    error?.message ??
    fallback
  );
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  const establishSession = useCallback(async (response) => {
    const token = getToken(response);

    if (!token) {
      throw new Error(
        "The authentication response did not contain a JWT. Check /api/auth/login."
      );
    }

    saveAuthToken(token);

    try {
      const currentUserResponse = await getCurrentUser();
      const currentUser = getUser(currentUserResponse);

      if (!currentUser || typeof currentUser !== "object") {
        throw new Error("The current-user endpoint returned no user.");
      }

      setUser(currentUser);
      return currentUser;
    } catch (error) {
      // Never keep a session if the token was rejected or removed.
      if (!getAuthToken()) {
        clearAuthToken();
        setUser(null);

        throw new Error(
          "The JWT was not accepted by /api/auth/me. Check the backend JWT filter and token response."
        );
      }

      // If /auth/me had a temporary non-authentication failure,
      // a user object from the login response may be used as fallback.
      const responseUser = getUser(response);

      if (responseUser && typeof responseUser === "object") {
        setUser(responseUser);
        return responseUser;
      }

      clearAuthToken();
      setUser(null);
      throw error;
    }
  }, []);

  useEffect(() => {
    let active = true;

    async function restoreSession() {
      if (!getAuthToken()) {
        if (active) setLoading(false);
        return;
      }

      try {
        const response = await getCurrentUser();
        const currentUser = getUser(response);

        if (
          active &&
          currentUser &&
          typeof currentUser === "object"
        ) {
          setUser(currentUser);
        } else if (active) {
          clearAuthToken();
          setUser(null);
        }
      } catch {
        if (active) {
          clearAuthToken();
          setUser(null);
        }
      } finally {
        if (active) setLoading(false);
      }
    }

    restoreSession();

    return () => {
      active = false;
    };
  }, []);

  const login = useCallback(
    async (credentials) => {
      try {
        const response = await loginUser(credentials);
        return await establishSession(response);
      } catch (error) {
        throw new Error(
          getErrorMessage(error, "Unable to log in. Please try again.")
        );
      }
    },
    [establishSession]
  );

  const register = useCallback(
    async (formData) => {
      try {
        const response = await registerUser(formData);

        if (getToken(response)) {
          return await establishSession(response);
        }

        // Registration may create the account without issuing a JWT.
        const loginResponse = await loginUser({
          username: formData.username,
          usernameOrEmail: formData.username,
          identifier: formData.username,
          email: formData.email,
          password: formData.password,
        });

        return await establishSession(loginResponse);
      } catch (error) {
        throw new Error(
          getErrorMessage(error, "Unable to create your account.")
        );
      }
    },
    [establishSession]
  );

  const logout = useCallback(() => {
    clearAuthToken();
    setUser(null);
  }, []);

  const value = useMemo(
    () => ({
      user,
      loading,
      isAuthenticated: Boolean(user && getAuthToken()),
      login,
      register,
      logout,
    }),
    [user, loading, login, register, logout]
  );

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);

  if (!context) {
    throw new Error("useAuth must be used inside AuthProvider.");
  }

  return context;
}
