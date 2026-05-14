import requests

API_KEY = '8a2cf12c1dd2a7a9ad56cd788c16e5b0cc35dfec2380cf8a2ad8fdbef18a76d9'
url = 'http://apis.data.go.kr/B554287/DisabledPersonConvenientFacility/getFacInfoOpenApiJpEvalInfoList'
params = {
    'serviceKey': API_KEY,
    'wfcltId': '4213011000-3-08080001',
}
res = requests.get(url, params=params)
print(res.text)