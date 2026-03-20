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
    
    // Booking Code
    const bookingCodePara = document.createElement('p');
    bookingCodePara.style.cssText = 'margin-bottom: 10px;';
    bookingCodePara.innerHTML = `<strong>Booking Code:</strong> ${exitDetails.bookingCode || 'N/A'}`;
    modalContent.appendChild(bookingCodePara);
    
    // Total Amount
    const totalFeePara = document.createElement('p');
    totalFeePara.style.cssText = 'margin-bottom: 10px;';
    const totalFee = Number(exitDetails.totalFee || 0);
    totalFeePara.innerHTML = `<strong>Total Amount:</strong> ₹${totalFee.toFixed(2)}`;
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
