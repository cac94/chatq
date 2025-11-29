import {
    Chart as ChartJS,
    CategoryScale,
    LinearScale,
    PointElement,
    LineElement,
    BarElement,
    Title,
    Tooltip,
    Legend,
    ArcElement,
    RadialLinearScale,
} from 'chart.js'
import { Chart } from 'react-chartjs-2'
import Modal from './Modal'

ChartJS.register(
    CategoryScale,
    LinearScale,
    PointElement,
    LineElement,
    BarElement,
    Title,
    Tooltip,
    Legend,
    ArcElement,
    RadialLinearScale
)

const ChartResultModal = ({ isOpen, onClose, chartData, chartType = 'bar' }) => {
    if (!isOpen || !chartData) return null

    // Ensure chartData has datasets
    const safeChartData = chartData.datasets ? chartData : { labels: [], datasets: [] }

    const options = {
        responsive: true,
        plugins: {
            legend: {
                position: 'top',
                labels: {
                    color: 'white'
                }
            },
            title: {
                display: true,
                text: 'Generated Chart',
                color: 'white'
            },
        },
        scales: {
            x: {
                ticks: { color: 'white' },
                grid: { color: '#475569' }
            },
            y: {
                ticks: { color: 'white' },
                grid: { color: '#475569' }
            }
        }
    }

    // Adjust options for specific chart types if needed
    if (chartType === 'pie' || chartType === 'doughnut' || chartType === 'polarArea' || chartType === 'radar') {
        delete options.scales
        
        // Generate random colors for each label
        const labelCount = safeChartData.labels?.length || 0
        const backgroundColors = []
        const borderColors = []
        
        for (let i = 0; i < labelCount; i++) {
            const r = Math.floor(Math.random() * 255)
            const g = Math.floor(Math.random() * 255)
            const b = Math.floor(Math.random() * 255)
            backgroundColors.push(`rgba(${r}, ${g}, ${b}, 0.7)`)
            borderColors.push(`rgba(${r}, ${g}, ${b}, 1)`)
        }
        
        // Apply colors to all datasets
        if (safeChartData.datasets) {
            safeChartData.datasets.forEach(dataset => {
                dataset.backgroundColor = backgroundColors
                dataset.borderColor = borderColors
                dataset.borderWidth = 1
            })
        }
    }

    return (
        <Modal isOpen={isOpen} onClose={onClose}>
            <div className="w-full max-w-4xl mx-auto bg-slate-800 p-4 rounded-lg">
                <div className="mb-4">
                    <h2 className="text-xl font-bold text-white">Chart Result</h2>
                </div>
                <div className="bg-slate-900 p-4 rounded-lg min-h-[400px] flex items-center justify-center">
                    <Chart type={chartType} data={safeChartData} options={options} />
                </div>
            </div>
        </Modal>
    )
}

export default ChartResultModal
