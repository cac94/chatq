import { useState, useEffect, useRef } from 'react'
import Modal from './Modal'

const ChartPromptModal = ({ isOpen, onClose, onSubmit, language = 'en', translations }) => {
    const [prompt, setPrompt] = useState('')
    const [chartType, setChartType] = useState('bar')
    const inputRef = useRef(null)

    useEffect(() => {
        if (isOpen) {
            setPrompt('')
            setChartType('bar')
            // Focus input after a short delay to ensure modal is rendered
            setTimeout(() => {
                inputRef.current?.focus()
            }, 100)
        }
    }, [isOpen])

    const handleSubmit = (e) => {
        e.preventDefault()
        // Prompt is optional now
        onSubmit(prompt, chartType)
        onClose()
    }

    const t = translations && translations[language] ? translations[language] : {
        chartPromptTitle: 'Generate Chart',
        chartPromptPlaceholder: 'Describe the chart you want to create...',
        cancel: 'Cancel',
        generate: 'Generate',
        chartTypeLabel: 'Chart Type',
        chartTypes: {
            bar: 'Bar',
            line: 'Line',
            pie: 'Pie',
            doughnut: 'Doughnut',
            radar: 'Radar',
            polarArea: 'Polar Area',
            bubble: 'Bubble',
            scatter: 'Scatter'
        }
    }

    // Fallback if specific keys are missing
    const title = t.chartPromptTitle || 'Generate Chart'
    const placeholder = t.chartPromptPlaceholder || 'Describe the chart you want to create...'
    const cancelText = t.cancel || 'Cancel'
    const generateText = t.generate || 'Generate'
    const chartTypeLabel = t.chartTypeLabel || 'Chart Type'
    const chartTypes = t.chartTypes || {
        bar: 'Bar',
        line: 'Line',
        pie: 'Pie',
        doughnut: 'Doughnut',
        radar: 'Radar',
        polarArea: 'Polar Area',
        bubble: 'Bubble',
        scatter: 'Scatter'
    }

    const availableChartTypes = ['bar', 'line', 'pie', 'doughnut', 'radar', 'polarArea', 'bubble', 'scatter']

    return (
        <Modal isOpen={isOpen} onClose={onClose}>
            <div className="w-full max-w-lg mx-auto">
                <h2 className="text-xl font-bold text-white mb-4">{title}</h2>
                <form onSubmit={handleSubmit}>
                    <div className="mb-4">
                        <label className="block text-slate-300 text-sm font-bold mb-2" htmlFor="chartType">
                            {chartTypeLabel}
                        </label>
                        <select
                            id="chartType"
                            value={chartType}
                            onChange={(e) => setChartType(e.target.value)}
                            className="w-full p-2 rounded bg-slate-700 text-white border border-slate-600 focus:outline-none focus:border-blue-500"
                        >
                            {availableChartTypes.map(type => (
                                <option key={type} value={type}>
                                    {chartTypes[type] || type}
                                </option>
                            ))}
                        </select>
                    </div>
                    <div className="mb-4">
                        <textarea
                            ref={inputRef}
                            value={prompt}
                            onChange={(e) => setPrompt(e.target.value)}
                            placeholder={placeholder}
                            className="w-full p-3 rounded-lg bg-slate-700 text-white border border-slate-600 focus:outline-none focus:border-blue-500 min-h-[100px] resize-none"
                            onKeyDown={(e) => {
                                if (e.key === 'Enter' && !e.shiftKey) {
                                    e.preventDefault()
                                    handleSubmit(e)
                                }
                            }}
                        />
                    </div>
                    <div className="flex justify-end gap-2">
                        <button
                            type="button"
                            onClick={onClose}
                            className="px-4 py-2 rounded bg-slate-600 hover:bg-slate-500 text-white transition-colors"
                        >
                            {cancelText}
                        </button>
                        <button
                            type="submit"
                            className="px-4 py-2 rounded transition-colors text-white bg-blue-600 hover:bg-blue-500"
                        >
                            {generateText}
                        </button>
                    </div>
                </form>
            </div>
        </Modal>
    )
}

export default ChartPromptModal
