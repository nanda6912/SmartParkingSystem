// Missing showReleaseConfirmation method for ExitManagement class
// Add this method to the ExitManagement class in exit.html

function showReleaseConfirmation(exitDetails) {
    const modal = document.createElement('div');
    modal.style.cssText = `
        position: fixed;
        top: 0;
        left: 0;
        width: 100%;
        height: 100%;
        background: rgba(0, 0, 0, 0.5);
        display: flex;
        justify-content: center;
        align-items: center;
        z-index: 10000;
    `;
    
    // Create modal content safely without XSS
    const modalContent = document.createElement('div');
    modalContent.style.cssText = 'background: white; padding: 30px; border-radius: 10px; text-align: center; max-width: 400px;';
    
    // Title
    const title = document.createElement('h3');
    title.style.cssText = 'color: #28a745; margin-bottom: 20px;';
    title.textContent = '✅ Exit Processed Successfully!';
    modalContent.appendChild(title);
    
    // Booking Code - Safe HTML construction
    const bookingCodePara = document.createElement('p');
    bookingCodePara.style.cssText = 'margin-bottom: 10px;';
    
    // Create strong element for label
    const bookingCodeLabel = document.createElement('strong');
    bookingCodeLabel.textContent = 'Booking Code: ';
    
    // Create text node for booking code value (safe from XSS)
    const bookingCodeValue = document.createTextNode(exitDetails.bookingCode || 'N/A');
    
    // Append label and value safely
    bookingCodePara.appendChild(bookingCodeLabel);
    bookingCodePara.appendChild(bookingCodeValue);
    modalContent.appendChild(bookingCodePara);
    
    // Total Amount - Safe numeric validation
    const totalFeePara = document.createElement('p');
    totalFeePara.style.cssText = 'margin-bottom: 10px;';
    
    // Explicitly convert and validate numeric input
    let totalFee = 0;
    const rawFee = exitDetails.totalFee;
    if (rawFee !== null && rawFee !== undefined && rawFee !== '') {
        const parsedFee = Number(rawFee);
        if (Number.isFinite(parsedFee) && !Number.isNaN(parsedFee)) {
            totalFee = parsedFee;
        }
    }
    
    // Create strong element for label
    const totalFeeLabel = document.createElement('strong');
    totalFeeLabel.textContent = 'Total Amount: ';
    
    // Create text node for total fee value (safe from XSS)
    const totalFeeValue = document.createTextNode(`₹${totalFee.toFixed(2)}`);
    
    // Append label and value safely
    totalFeePara.appendChild(totalFeeLabel);
    totalFeePara.appendChild(totalFeeValue);
    modalContent.appendChild(totalFeePara);
    
    // Description
    const descPara = document.createElement('p');
    descPara.style.cssText = 'margin-bottom: 20px; color: #666;';
    descPara.textContent = 'The vehicle has been released and the parking slot is now available. Download your receipt below.';
    modalContent.appendChild(descPara);
    
    // Question
    const questionDiv = document.createElement('div');
    questionDiv.style.cssText = 'margin-bottom: 20px;';
    const questionPara = document.createElement('p');
    questionPara.style.cssText = 'margin-bottom: 10px;';
    questionPara.innerHTML = '<strong>Would you like to download the receipt?</strong>';
    questionDiv.appendChild(questionPara);
    modalContent.appendChild(questionDiv);
    
    // Buttons container
    const buttonsDiv = document.createElement('div');
    buttonsDiv.style.cssText = 'display: flex; gap: 10px; justify-content: center;';
    
    // Download Receipt Button
    const downloadBtn = document.createElement('button');
    downloadBtn.style.cssText = 'background: #28a745; color: white; border: none; padding: 10px 20px; border-radius: 5px; cursor: pointer;';
    downloadBtn.textContent = '📄 Download Receipt';
    downloadBtn.addEventListener('click', () => {
        downloadReceiptAndClose(exitDetails.bookingId, exitDetails.bookingCode);
    });
    buttonsDiv.appendChild(downloadBtn);
    
    // Close Button
    const closeBtn = document.createElement('button');
    closeBtn.style.cssText = 'background: #6c757d; color: white; border: none; padding: 10px 20px; border-radius: 5px; cursor: pointer;';
    closeBtn.textContent = 'Close';
    closeBtn.addEventListener('click', () => {
        closeReleaseConfirmation();
    });
    buttonsDiv.appendChild(closeBtn);
    
    modalContent.appendChild(buttonsDiv);
    modal.appendChild(modalContent);

    document.body.appendChild(modal);
    window.currentReleaseModal = modal;
}

window.downloadReceiptAndClose = function(bookingId, bookingCode) {
    // Download receipt using booking code for better filename
    window.downloadExitReceiptByCode(bookingCode);
    
    // Show success message
    setTimeout(() => {
        alert('Receipt downloaded successfully!');
        
        // Close the modal
        window.closeReleaseConfirmation();
    }, 500);
};

window.closeReleaseConfirmation = function() {
    if (window.currentReleaseModal) {
        document.body.removeChild(window.currentReleaseModal);
        window.currentReleaseModal = null;
    }
};
