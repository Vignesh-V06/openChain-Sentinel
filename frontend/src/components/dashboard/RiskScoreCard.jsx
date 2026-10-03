function RiskScoreCard({
  score = 0,
  risk = "UNKNOWN",
}) {
  const safeScore = Math.max(
    0,
    Math.min(100, Number(score) || 0)
  );

  const normalizedRisk =
    String(risk).toUpperCase();

  const riskClass =
    normalizedRisk === "CRITICAL" ||
    normalizedRisk === "HIGH"
      ? "high"
      : normalizedRisk === "MEDIUM"
      ? "medium"
      : "low";

  const getRiskMessage = () => {
    if (safeScore >= 75) {
      return "Immediate attention recommended";
    }

    if (safeScore >= 50) {
      return "Security improvements recommended";
    }

    if (safeScore >= 25) {
      return "Some security issues require review";
    }

    return "Dependency security posture is healthy";
  };

  return (
    <section className="risk-card">

      <div className="risk-card-header">

        <div>
          <div className="eyebrow">
            SECURITY POSTURE
          </div>

          <h2>
            Overall project risk
          </h2>
        </div>

        <span
          className={`risk-pill ${riskClass}`}
        >
          {normalizedRisk}
        </span>

      </div>


      <div className="risk-score-area">

        <div
          className="risk-ring"
          style={{
            "--progress": `${safeScore}%`,
          }}
        >

          <div className="risk-ring-inner">

            <strong>
              {safeScore}
            </strong>

            <span>
              / 100
            </span>

          </div>

        </div>


        <div className="risk-explanation">

          <div className="risk-main-text">
            {getRiskMessage()}
          </div>

          <p>
            Sentinel combines vulnerability
            severity with dependency context,
            affected paths and available fixes
            to calculate project-specific risk.
          </p>

          <div className="risk-context">

            <div>
              <span>
                Risk model
              </span>

              <strong>
                Contextual
              </strong>
            </div>

            <div>
              <span>
                Analysis
              </span>

              <strong>
                Dependency graph
              </strong>
            </div>

            <div>
              <span>
                Intelligence
              </span>

              <strong>
                OSV
              </strong>
            </div>

          </div>

        </div>

      </div>

    </section>
  );
}

export default RiskScoreCard;