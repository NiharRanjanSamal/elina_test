import PropTypes from 'prop-types'

const currency = new Intl.NumberFormat('en-IN', {
  style: 'currency',
  currency: 'INR',
  minimumFractionDigits: 0
})

const ResourceSummaryWidget = ({ summary }) => {
  const cards = [
    {
      label: 'Manpower Cost',
      value: summary?.manpowerCost ?? 0,
      color: 'bg-blue-50 text-blue-700 border-blue-200'
    },
    {
      label: 'Equipment Cost',
      value: summary?.equipmentCost ?? 0,
      color: 'bg-amber-50 text-amber-700 border-amber-200'
    },
    {
      label: 'Total Cost',
      value: summary?.totalCost ?? 0,
      color: 'bg-emerald-50 text-emerald-700 border-emerald-200'
    }
  ]

  return (
    <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
      {cards.map((card) => (
        <div
          key={card.label}
          className={`rounded-xl border px-4 py-5 shadow-sm transition hover:shadow-md ${card.color}`}
        >
          <p className="text-sm font-medium uppercase tracking-wide text-gray-500">{card.label}</p>
          <p className="mt-2 text-2xl font-semibold">{currency.format(card.value)}</p>
        </div>
      ))}
    </div>
  )
}

ResourceSummaryWidget.propTypes = {
  summary: PropTypes.shape({
    manpowerCost: PropTypes.number,
    equipmentCost: PropTypes.number,
    totalCost: PropTypes.number
  })
}

export default ResourceSummaryWidget

