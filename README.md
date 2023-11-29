# Project-time-tracker
CLI Tool for time tracking, summary functionality and on-demand upload to numerous sites

# Setup
Create config file
```
java -jar app.jar init --configpath ./app.json --csvpath ./my_times.json
Successfully created config file: ./app.json
```

Edit parameters according to your needs. e.g.:
```
{
    "child_sick_leave": [],
    "chrome_profile_path": "",
    "cost_assessment_setup": { 
        "absence_projects": {},
        "development_projects": {},
        "internal_projects": {},
        "maintenance_projects": {}
    },
    "csv_path": "./my_times.json",
    "days_off": [
        "2023-22-27", 
        "2023-12-05"],
    "e_time_language": "EN",
    "e_time_url": "https://url-to-cost-assessment-app.com",
    "max_work_duration_till_auto_clockout": "PT8H30M",
    "my_hr_self_service_language": "EN",
    "my_hr_self_service_url": "https://url-to-time-keeping-app.com",
    "sick_leave": [
        "2023-09-08_2023-09-08",
        "2023-09-19_2023-09-20"],
    "standard_work_duration_per_day": "PT8H",
    "vacation": [
        "2023-05-19_2023-05-19",
        "2023-07-17_2023-08-04"],
    "weekdays_off": "SATURDAY,SUNDAY"
}
```
We will cover the cost_assessment_setup section later

If you do NOT want to use the Webdriver functionality, the following parameters are irrelevant:
- my_hr_self_service_url
- my_hr_self_service_language
- e_time_url
- e_time_language
- chrome_profile_path

# Usage 
tbd