function SeverityChart({
  critical = 0,
  high = 0,
  medium = 0,
  low = 0,
}) {
  const total =
    critical +
    high +
    medium +
    low;

  const severityData = [
    {
      label: "Critical",
      value: critical,
      className: "critical",
    },
    {
      label: "High",
      value: high,
      className: "high",
    },
    {
      label: "Medium",
      value: medium,
      className: "medium",
    },
    {
      label: "Low",
      value: low,
      className: "low",
    },
  ];

  const percentage = (value) => {
    if (total === 0) {
      return 0;
    }

    return Math.round(
      (value / total) * 100
    );
  };

  return (
    <div className="panel severity-panel">

      <div className="panel-header">

        <div>
          <div className="eyebrow">
            SECURITY FINDINGS
          </div>

          <h3>
            Severity distribution
          </h3>
        </div>

        <span className="panel-count">
          {total} total
        </span>

      </div>


      {total === 0 ? (
        <div className="severity-empty">
          No vulnerability findings
          for this scan.
        </div>
      ) : (

        <div className="severity-bars">

          {severityData.map((item) => {

            const percent =
              percentage(item.value);

            return (
              <div
                className="severity-row"
                key={item.label}
              >

                <div className="severity-label">

                  <div className="severity-name">

                    <span
                      className={`severity-dot ${item.className}`}
                    />

                    <span>
                      {item.label}
                    </span>

                  </div>

                  <strong>
                    {item.value}
                  </strong>

                </div>


                <div className="severity-track">

                  <div
                    className={`severity-fill ${item.className}`}
                    style={{
                      width: `${percent}%`,
                    }}
                  />

                </div>

              </div>
            );
          })}

        </div>

      )}

    </div>
  );
}

export default SeverityChart;