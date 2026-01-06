
import React from 'react';

const ProgressRing = ({ radius, stroke, progress, goal, children }) => {
    const normalizedRadius = radius - stroke * 2;
    const circumference = normalizedRadius * 2 * Math.PI;

    // Ensure progress doesn't exceed 100% physically for the circle (visual clamp), 
    // but we can show >100% text inside.
    const percentage = Math.min(100, (progress / goal) * 100);
    const strokeDashoffset = circumference - (percentage / 100) * circumference;

    const isOverLimit = progress > goal;

    return (
        <div className="relative flex items-center justify-center">
            <svg
                height={radius * 2}
                width={radius * 2}
                className="transform -rotate-90"
            >
                <circle
                    stroke="rgba(255,255,255,0.1)"
                    strokeWidth={stroke}
                    fill="transparent"
                    r={normalizedRadius}
                    cx={radius}
                    cy={radius}
                />
                <circle
                    stroke={isOverLimit ? '#ef4444' : '#f43f5e'} // Red if over, Rose if under
                    fill="transparent"
                    strokeWidth={stroke}
                    strokeDasharray={circumference + ' ' + circumference}
                    style={{ strokeDashoffset, transition: 'stroke-dashoffset 0.5s ease-in-out' }}
                    strokeLinecap="round"
                    r={normalizedRadius}
                    cx={radius}
                    cy={radius}
                />
            </svg>
            <div className="absolute flex flex-col items-center justify-center text-center">
                {children}
            </div>
        </div>
    );
};

export default ProgressRing;
