function StatCard({
  label,
  value,
  description,
  icon: Icon,
  accent = "blue",
}) {
  return (
    <div
      className={`stat-card stat-card-${accent}`}
    >

      <div className="stat-card-top">

        <div className="stat-icon">
          {Icon && (
            <Icon size={15} />
          )}
        </div>

        <span className="stat-label">
          {label}
        </span>

      </div>

      <div className="stat-value">
        {value}
      </div>

      <div className="stat-description">
        {description}
      </div>

    </div>
  );
}

export default StatCard;