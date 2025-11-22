import PropTypes from 'prop-types'

const dayDiff = (start, end) => {
  const s = new Date(start)
  const e = new Date(end)
  return Math.max(Math.round((e - s) / (1000 * 60 * 60 * 24)), 0)
}

const AllocationTimeline = ({ timeline, wbsStart, wbsEnd }) => {
  if (!wbsStart || !wbsEnd) {
    return (
      <div className="rounded-lg border border-dashed border-gray-300 p-4 text-sm text-gray-500">
        WBS dates are required to render timeline.
      </div>
    )
  }

  const totalDays = dayDiff(wbsStart, wbsEnd) + 1

  const toPercent = (value) => {
    if (totalDays <= 0) return 0
    return Math.min(Math.max((value / totalDays) * 100, 0), 100)
  }

  const baseline = new Date(wbsStart)

  const computeOffset = (startDate) => {
    const diff = dayDiff(baseline, startDate)
    return toPercent(diff)
  }

  const typeColors = {
    MANPOWER: 'bg-blue-500',
    EQUIPMENT: 'bg-amber-500'
  }

  return (
    <div className="space-y-4">
      {timeline.length === 0 && (
        <p className="text-sm text-gray-500">No allocations scheduled for this WBS.</p>
      )}
      {timeline.map((item) => {
        const offset = computeOffset(item.startDate)
        const width = toPercent(item.durationDays ?? dayDiff(item.startDate, item.endDate) + 1)
        return (
          <div key={`${item.resourceType}-${item.resourceName}-${item.startDate}`} className="space-y-1">
            <div className="flex items-center justify-between text-sm">
              <span className="font-medium text-gray-800">
                {item.resourceName}{' '}
                <span className="text-xs text-gray-500">({item.resourceType.toLowerCase()})</span>
              </span>
              <span className="text-xs text-gray-500">
                {item.startDate} → {item.endDate}
              </span>
            </div>
            <div className="relative h-4 rounded-full bg-gray-100">
              <div
                className={`absolute h-4 rounded-full ${typeColors[item.resourceType] ?? 'bg-slate-500'}`}
                style={{ left: `${offset}%`, width: `${width}%` }}
              />
            </div>
          </div>
        )
      })}
    </div>
  )
}

AllocationTimeline.propTypes = {
  timeline: PropTypes.arrayOf(
    PropTypes.shape({
      resourceName: PropTypes.string.isRequired,
      resourceType: PropTypes.string.isRequired,
      startDate: PropTypes.string.isRequired,
      endDate: PropTypes.string.isRequired,
      durationDays: PropTypes.number
    })
  ),
  wbsStart: PropTypes.string,
  wbsEnd: PropTypes.string
}

export default AllocationTimeline

