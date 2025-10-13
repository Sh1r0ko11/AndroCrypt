using System;
using System.Windows;

namespace CVE_DashBoard_AndroCrypt
{
    public partial class MainWindow : Window
    {
        public MainWindow()
        {
            InitializeComponent();
            Loaded += MainWindow_Loaded;
        }

        private void MainWindow_Loaded(object sender, RoutedEventArgs e)
        {
            webView.Source = new Uri("https://www.cve.org/CVERecord/SearchResults?query=android");
        }

        private void RefreshButton_Click(object sender, RoutedEventArgs e)
        {
            webView.Reload();
        }
    }
}