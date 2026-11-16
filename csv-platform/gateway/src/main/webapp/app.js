const API_BASE = "/gateway/api";

let jobId = null;

const fileInput = document.querySelector("#file-input");
const fileName = document.querySelector("#file-name");
const analyzeButton = document.querySelector("#analyze-button");
const clearButton = document.querySelector("#clear-button");
const downloadButton = document.querySelector("#download-button");
const uploadControls = document.querySelector("#upload-controls");
const errorBox = document.querySelector("#error-box");
const results = document.querySelector("#results");
const summaryChips = document.querySelector("#summary-chips");
const issuesBody = document.querySelector("#issues-body");

fileInput.addEventListener("change", () => {
    const file = fileInput.files[0];
    fileName.textContent = file ? file.name : "No file selected";
    analyzeButton.disabled = !file;
    hideError();
});

analyzeButton.addEventListener("click", uploadFile);
clearButton.addEventListener("click", clearJob);
downloadButton.addEventListener("click", downloadCleanedFile);

window.addEventListener("pagehide", () => {
    if (jobId) {
        navigator.sendBeacon(`${API_BASE}/clear?jobId=${encodeURIComponent(jobId)}`);
    }
});

loadServiceHealth();

async function uploadFile() {
    const file = fileInput.files[0];
    if (!file) {
        return;
    }

    hideError();
    analyzeButton.disabled = true;
    analyzeButton.textContent = "Analyzing…";

    const formData = new FormData();
    formData.append("file", file);

    try {
        const response = await fetch(`${API_BASE}/upload`, {
            method: "POST",
            body: formData
        });

        if (!response.ok) {
            showError(await response.text());
            return;
        }

        let report;
        try {
            report = await response.json();
        } catch {
            showError("Invalid report received from gateway");
            return;
        }

        if (!report || typeof report.jobId !== "string" || !report.jobId) {
            showError("Invalid report received from gateway");
            return;
        }

        jobId = report.jobId;
        renderReport(report);
        results.hidden = false;
        uploadControls.hidden = true;
        clearButton.hidden = false;
        downloadButton.disabled = false;
    } catch {
        showError("Cannot reach the gateway");
    } finally {
        analyzeButton.textContent = "Analyze";
        analyzeButton.disabled = !fileInput.files[0];
    }
}

async function clearJob() {
    if (!jobId) {
        return;
    }

    hideError();
    clearButton.disabled = true;

    try {
        const response = await fetch(
            `${API_BASE}/clear?jobId=${encodeURIComponent(jobId)}`,
            { method: "POST" }
        );
        if (!response.ok) {
            showError(await response.text());
            return;
        }

        jobId = null;
        fileInput.value = "";
        fileName.textContent = "No file selected";
        analyzeButton.disabled = true;
        results.hidden = true;
        downloadButton.disabled = true;
        summaryChips.replaceChildren();
        issuesBody.replaceChildren();
        uploadControls.hidden = false;
        clearButton.hidden = true;
    } catch {
        showError("Cannot reach the gateway");
    } finally {
        clearButton.disabled = false;
    }
}

async function downloadCleanedFile() {
    if (!jobId) {
        return;
    }

    hideError();
    downloadButton.disabled = true;
    try {
        const response = await fetch(
            `${API_BASE}/download?jobId=${encodeURIComponent(jobId)}`
        );
        if (!response.ok) {
            showError(await response.text());
            return;
        }

        const objectUrl = URL.createObjectURL(await response.blob());
        const link = document.createElement("a");
        link.href = objectUrl;
        link.download = "cleaned.csv";
        document.body.append(link);
        link.click();
        link.remove();
        window.setTimeout(() => URL.revokeObjectURL(objectUrl), 0);
    } catch {
        showError("Cannot reach the gateway");
    } finally {
        downloadButton.disabled = !jobId;
    }
}

function renderReport(report) {
    summaryChips.replaceChildren();
    issuesBody.replaceChildren();

    const file = report.file || {};
    addChip("Rows", file.rows ?? 0);
    addChip("Columns", file.columns ?? 0);

    const categories = report.summary?.byCategory || {};
    for (const [category, count] of Object.entries(categories)) {
        addChip(formatLabel(category), count);
    }

    const issues = Array.isArray(report.issues) ? report.issues : [];
    if (issues.length === 0) {
        const row = document.createElement("tr");
        const cell = document.createElement("td");
        cell.colSpan = 4;
        cell.className = "empty-state";
        cell.textContent = "No issues found in this file.";
        row.append(cell);
        issuesBody.append(row);
        return;
    }

    for (const issue of issues) {
        const row = document.createElement("tr");
        appendCell(row, formatLabel(issue.category));
        appendCell(row, issue.column == null ? "-" : issue.column);
        appendCell(row, formatRows(issue.rows));
        appendCell(row, formatLabel(issue.suggestedAction));
        issuesBody.append(row);
    }
}

function addChip(label, value) {
    const chip = document.createElement("div");
    chip.className = "summary-chip";

    const count = document.createElement("strong");
    count.textContent = String(value);
    const name = document.createElement("span");
    name.textContent = label;

    chip.append(count, name);
    summaryChips.append(chip);
}

function appendCell(row, value) {
    const cell = document.createElement("td");
    cell.textContent = value == null || value === "" ? "—" : String(value);
    row.append(cell);
}

function formatRows(rows) {
    if (!Array.isArray(rows) || rows.length === 0) {
        return "—";
    }

    const visibleRows = rows.slice(0, 10).join(", ");
    const remaining = rows.length - 10;
    return remaining > 0 ? `${visibleRows} +${remaining} more` : visibleRows;
}

function formatLabel(value) {
    return value == null ? "—" : String(value).replaceAll("_", " ");
}

function showError(message) {
    errorBox.textContent = message || "The request failed";
    errorBox.hidden = false;
}

function hideError() {
    errorBox.textContent = "";
    errorBox.hidden = true;
}

async function loadServiceHealth() {
    try {
        const response = await fetch(`${API_BASE}/health`);
        if (!response.ok) {
            return;
        }

        const health = await response.json();
        for (const service of ["gateway", "analysis", "cleaning"]) {
            const dot = document.querySelector(`[data-service="${service}"]`);
            const status = health[service];
            if (status === "UP" || status === "DOWN") {
                dot.classList.add(status === "UP" ? "is-up" : "is-down");
                dot.setAttribute("aria-label", `${service} ${status.toLowerCase()}`);
            }
        }
    } catch {
        // Keep the indicators grey when the health request cannot be completed.
    }
}
