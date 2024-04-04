function getReport(){
    // Get the form element
    var form = document.getElementById("reportForm");

    // Get the selected report type
    var reportType = form.elements["report_type"].value;

    // Get the selected call type
    var callType = form.elements["call_type"].value;

    // Get the email parameter
    var parameter = form.elements["parameter"].value;

    // Log the selected values (you can modify this part as needed)
    console.log("Report Type:", reportType);
    console.log("Call Type:", callType);
    console.log("Email Parameter:", parameter);

    var reqBody= {
        "byEmail":parameter
    }

    showLoader();
    
    fetch("http://localhost:4444/api/jasper/http",{
        method:"POST", 
        body: JSON.stringify(reqBody) ,    
        mode: 'cors',  
        headers:{  
            'Content-Type':"application/json"
        }
    }).then((response)=>{
        if(!response.ok){
            throw response;
        }else{
            return response.json();
        }
    })
    .then((data)=>{
        console.log(data);
        hideLoader();
    })
    .catch((err)=>console.error("Error: "+ err));


    function showLoader() {
        document.getElementById('loader').style.display = 'block';
    }

    // Hide loader function
    function hideLoader() {
        document.getElementById('loader').style.display = 'none';
    }


    
}