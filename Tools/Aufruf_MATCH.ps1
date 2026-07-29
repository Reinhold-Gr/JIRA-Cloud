cd C:\GitHub\Reinhold-Gr\JIRA-Cloud\Tools
.\MATCH_CF_DC-CL.ps1 `
    -DcPath "C:\GitHub\Reinhold-Gr\JIRA-Cloud\Tools\Customfileds_DC.txt" `
    -CloudPath "C:\GitHub\Reinhold-Gr\JIRA-Cloud\Tools\Customfilelds_CL.json" `
    -OutPath "C:\GitHub\Reinhold-Gr\JIRA-Cloud\Tools\CustomFieldMapping.json" `
    -UnmatchedDcPath "C:\GitHub\Reinhold-Gr\JIRA-Cloud\Tools\Unmatched_DC.json" `
    -UnmatchedClPath "C:\GitHub\Reinhold-Gr\JIRA-Cloud\Tools\Unmatched_CL.json"