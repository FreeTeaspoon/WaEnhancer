package com.freeteaspoon.wppenhacer.ui.miuix

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import com.freeteaspoon.wppenhacer.R
import top.yukonga.miuix.kmp.preference.ArrowPreference

@Composable
internal fun ManagerCreditsScreen(onBack: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    val open: (String) -> Unit = { uriHandler.openUri(it) }
    val contributors = listOf("Dev4Mod", "frknkrc44", "mubashardev", "masbentoooredoo", "zhongerxll", "BryanGIG", "rizqi-developer", "pedroborraz", "ahmedtohamy1", "mohdafix", "maulana-kurniawan", "erzachn", "cvnertnc", "rkorossy", "StupidRepo", "Blank517", "astola-studio", "Strange-IPmart")
    val licenceTitle = stringResource(R.string.manager_licence)
    val contributorsTitle = stringResource(R.string.manager_contributors)
    ManagerDetailScaffold(stringResource(R.string.manager_credits), false, onBack) {
        item("tip", contentType = PageStart.Inset) { ManagerTipCard(stringResource(R.string.manager_about_credits)) }
        managerSection(licenceTitle, "licence")
        item("links") { ManagerGroupCard {
            ArrowPreference(stringResource(R.string.manager_licence), summary = stringResource(R.string.manager_licence_summary), onClick = { open("https://www.gnu.org/licenses/gpl-3.0.html") })
            ArrowPreference(stringResource(R.string.manager_support_channel), onClick = { open("https://t.me/waenhancer") })
        } }
        managerSection(contributorsTitle, "contributors")
        managerGroupedCardItems("contributors", contributors.map { name ->
            ManagerCardItem(name) { ArrowPreference(name, onClick = { open("https://github.com/$name") }) }
        })
    }
}
