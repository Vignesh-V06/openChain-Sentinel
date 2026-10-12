
import { useState } from "react";
import { Link, Navigate, useNavigate } from "react-router-dom";
import {
  ArrowRight,
  Eye,
  EyeOff,
  GitBranch,
  LoaderCircle,
  ShieldCheck,
} from "lucide-react";

import { useAuth } from "../context/AuthContext";
import "./AuthPage.css";

function AuthPage({ mode = "login" }) {
  const isRegister = mode === "register";
  const navigate = useNavigate();
  const { login, register, isAuthenticated, loading } = useAuth();

  const [form, setForm] = useState({
    displayName: "",
    username: "",
    email: "",
    password: "",
    confirmPassword: "",
  });

  const [showPassword, setShowPassword] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");

  if (loading) {
    return (
      <div className="auth-loading">
        <LoaderCircle className="auth-spinner" size={24} />
        <span>Restoring your session...</span>
      </div>
    );
  }

  if (isAuthenticated) {
    return <Navigate to="/dashboard" replace />;
  }

  function handleChange(event) {
    const { name, value } = event.target;

    setForm((current) => ({
      ...current,
      [name]: value,
    }));
  }

  async function handleSubmit(event) {
    event.preventDefault();
    setError("");

    if (isRegister && form.password !== form.confirmPassword) {
      setError("Your passwords do not match.");
      return;
    }

    if (form.password.length < 8) {
      setError("Use a password with at least 8 characters.");
      return;
    }

    setSubmitting(true);

    try {
      if (isRegister) {
        await register({
          displayName: form.displayName.trim(),
          username: form.username.trim(),
          email: form.email.trim(),
          password: form.password,
        });
      } else {
        const identifier = form.username.trim();

        // Include common identifier field names so this UI can work
        // with username- or email-based login DTOs.
        await login({
          username: identifier,
          usernameOrEmail: identifier,
          identifier,
          email: identifier,
          password: form.password,
        });
      }

      navigate("/dashboard", { replace: true });
    } catch (submitError) {
      setError(
        submitError?.message ||
          "Something went wrong. Please try again."
      );
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <main className="auth-page">
      <section className="auth-brand-panel">
        <div className="auth-brand">
          <div className="auth-brand-mark">
            <GitBranch size={25} />
          </div>
          <div>
            <div className="auth-brand-name">OpenChain</div>
            <div className="auth-brand-subtitle">S E N T I N E L</div>
          </div>
        </div>

        <div className="auth-brand-copy">
          <div className="auth-eyebrow">
            <span className="auth-eyebrow-dot" />
            SOFTWARE SUPPLY CHAIN INTELLIGENCE
          </div>

          <h1>
            See the chain.
            <br />
            <span>Understand the risk.</span>
          </h1>

          <p>
            Trace dependencies, understand vulnerability impact,
            and make informed decisions about your software supply chain.
          </p>

          <div className="auth-feature-list">
            <div className="auth-feature">
              <span className="auth-feature-icon">
                <GitBranch size={17} />
              </span>
              <div>
                <strong>Dependency visibility</strong>
                <span>Understand direct and transitive relationships.</span>
              </div>
            </div>

            <div className="auth-feature">
              <span className="auth-feature-icon">
                <ShieldCheck size={17} />
              </span>
              <div>
                <strong>Context-aware risk</strong>
                <span>See where vulnerabilities enter your project.</span>
              </div>
            </div>
          </div>
        </div>

        <div className="auth-brand-footer">
          OpenChain Sentinel · Dependency intelligence
        </div>
      </section>

      <section className="auth-form-panel">
        <div className="auth-form-container">
          <div className="auth-mobile-brand">
            <div className="auth-brand-mark">
              <GitBranch size={23} />
            </div>
            <span>OpenChain Sentinel</span>
          </div>

          <div className="auth-form-heading">
            <span className="auth-form-kicker">
              {isRegister ? "GET STARTED" : "WELCOME BACK"}
            </span>

            <h2>
              {isRegister ? "Create your account" : "Sign in to Sentinel"}
            </h2>

            <p>
              {isRegister
                ? "Set up your workspace to start analysing repository risk."
                : "Sign in to continue to your security workspace."}
            </p>
          </div>

          {error && (
            <div className="auth-error" role="alert">
              <span className="auth-error-mark">!</span>
              <span>{error}</span>
            </div>
          )}

          <form className="auth-form" onSubmit={handleSubmit}>
            {isRegister && (
              <>
                <div className="auth-field">
                  <label htmlFor="displayName">Full name</label>
                  <input
                    id="displayName"
                    name="displayName"
                    type="text"
                    placeholder="Your name"
                    value={form.displayName}
                    onChange={handleChange}
                    autoComplete="name"
                    required
                    maxLength={100}
                  />
                </div>

                <div className="auth-field">
                  <label htmlFor="username">Username</label>
                  <input
                    id="username"
                    name="username"
                    type="text"
                    placeholder="Choose a username"
                    value={form.username}
                    onChange={handleChange}
                    autoComplete="username"
                    required
                    minLength={3}
                    maxLength={50}
                  />
                </div>

                <div className="auth-field">
                  <label htmlFor="email">Email address</label>
                  <input
                    id="email"
                    name="email"
                    type="email"
                    placeholder="you@example.com"
                    value={form.email}
                    onChange={handleChange}
                    autoComplete="email"
                    required
                  />
                </div>
              </>
            )}

            {!isRegister && (
              <div className="auth-field">
                <label htmlFor="username">Username or email</label>
                <input
                  id="username"
                  name="username"
                  type="text"
                  placeholder="Enter your username or email"
                  value={form.username}
                  onChange={handleChange}
                  autoComplete="username"
                  required
                />
              </div>
            )}

            <div className="auth-field">
              <div className="auth-label-row">
                <label htmlFor="password">Password</label>
              </div>

              <div className="auth-password-wrap">
                <input
                  id="password"
                  name="password"
                  type={showPassword ? "text" : "password"}
                  placeholder={
                    isRegister ? "At least 8 characters" : "Enter your password"
                  }
                  value={form.password}
                  onChange={handleChange}
                  autoComplete={
                    isRegister ? "new-password" : "current-password"
                  }
                  required
                  minLength={8}
                />

                <button
                  type="button"
                  className="auth-password-toggle"
                  onClick={() => setShowPassword((current) => !current)}
                  aria-label={showPassword ? "Hide password" : "Show password"}
                >
                  {showPassword ? <EyeOff size={17} /> : <Eye size={17} />}
                </button>
              </div>
            </div>

            {isRegister && (
              <div className="auth-field">
                <label htmlFor="confirmPassword">Confirm password</label>
                <input
                  id="confirmPassword"
                  name="confirmPassword"
                  type={showPassword ? "text" : "password"}
                  placeholder="Re-enter your password"
                  value={form.confirmPassword}
                  onChange={handleChange}
                  autoComplete="new-password"
                  required
                  minLength={8}
                />
              </div>
            )}

            <button
              type="submit"
              className="auth-submit"
              disabled={submitting}
            >
              {submitting ? (
                <>
                  <LoaderCircle size={18} className="auth-spinner" />
                  {isRegister ? "Creating account..." : "Signing in..."}
                </>
              ) : (
                <>
                  {isRegister ? "Create account" : "Sign in"}
                  <ArrowRight size={17} />
                </>
              )}
            </button>
          </form>

          <div className="auth-switch">
            {isRegister ? (
              <>
                Already have an account?{" "}
                <Link to="/login">Sign in</Link>
              </>
            ) : (
              <>
                New to OpenChain Sentinel?{" "}
                <Link to="/register">Create an account</Link>
              </>
            )}
          </div>

          <div className="auth-security-note">
            <ShieldCheck size={15} />
            <span>Your workspace is protected by authenticated access.</span>
          </div>
        </div>
      </section>
    </main>
  );
}

export default AuthPage;
