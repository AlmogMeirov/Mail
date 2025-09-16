const axios = require('axios');

const BASE_URL = 'http://localhost:3000/api';
const AUTH_TOKEN = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiI2OGM2ODU4Mzk4ZTc0NmY2M2ViYWM4NzgiLCJlbWFpbCI6ImJAZ21haWwuY29tIiwiaWF0IjoxNzU3ODQxODIzLCJleHAiOjE3NTc4NDU0MjN9.otBm9Y5R1OCoKFqvZ5olB6prowZaw_XIusNSEQbvDaU'; // החלף בטוקן האמיתי שלך
const USER_EMAIL = 'b@gmail.com'; // החלף במייל שלך

async function sendEmail(index) {
    const subjects = [
        'דוח שבועי',
        'עדכון פרויקט',
        'פגישה חשובה',
        'הודעה דחופה',
        'מסמכים לעיון',
        'סיכום ישיבה',
        'בקשה לאישור',
        'עדכון סטטוס',
        'דוח חודשי',
        'הזמנה לאירוע'
    ];

    const contents = [
        'שלום, מצורף הדוח השבועי לבדיקתך.',
        'עדכון על התקדמות הפרויקט החדש.',
        'נדרשת נוכחותך בפגישה חשובה מחר.',
        'יש לטפל בנושא זה בהקדם האפשרי.',
        'מצורפים מסמכים חשובים לעיון.',
        'סיכום הנקודות העיקריות מהישיבה.',
        'אנא אשר את הבקשה הבאה.',
        'עדכון על סטטוס המשימות השונות.',
        'דוח מפורט על פעילות החודש.',
        'הזמנה רשמית לאירוע הקרוב.'
    ];

    const randomSubject = subjects[index % subjects.length];
    const randomContent = contents[index % contents.length];

    const emailData = {
        sender: USER_EMAIL,
        recipients: [USER_EMAIL], // שולח לעצמך
        subject: `${randomSubject} #${index + 1}`,
        content: `${randomContent}\n\nמייל מספר: ${index + 1}\nנשלח: ${new Date().toLocaleString('he-IL')}`,
        labels: ['inbox']
    };

    try {
        const response = await axios.post(`${BASE_URL}/mails`, emailData, {
            headers: {
                'Authorization': `Bearer ${AUTH_TOKEN}`,
                'Content-Type': 'application/json'
            }
        });
        
        console.log(`✅ מייל ${index + 1} נשלח בהצלחה`);
        return true;
    } catch (error) {
        console.error(`❌ שגיאה בשליחת מייל ${index + 1}:`, error.response?.status, error.response?.statusText);
        return false;
    }
}

// פונקציה ראשית
async function sendMultipleEmails() {
    console.log('🚀 מתחיל לשלוח 200 מיילים...');
    console.log('⚠️  וודא שהטוקן והמייל נכונים לפני הרצה!');
    
    let successCount = 0;
    let errorCount = 0;

    // שלח 200 מיילים עם השהיה קטנה ביניהם
    for (let i = 0; i < 200; i++) {
        const success = await sendEmail(i);
        
        if (success) {
            successCount++;
        } else {
            errorCount++;
        }

        // השהיה של 100ms בין מיילים כדי לא להעמיס על השרת
        await new Promise(resolve => setTimeout(resolve, 100));

        // הצג התקדמות כל 25 מיילים
        if ((i + 1) % 25 === 0) {
            console.log(`📊 התקדמות: ${i + 1}/200 (הצלחות: ${successCount}, שגיאות: ${errorCount})`);
        }
    }

    console.log('\n🎉 סיימתי!');
    console.log(`✅ הצלחות: ${successCount}`);
    console.log(`❌ שגיאות: ${errorCount}`);
    console.log('\n💡 עכשיו אתה יכול לבדוק את הפגינציה באפליקציה!');
}

// הרץ את הסקריפט
sendMultipleEmails().catch(console.error);